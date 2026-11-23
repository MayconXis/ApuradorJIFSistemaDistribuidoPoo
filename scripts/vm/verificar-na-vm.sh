#!/bin/bash
# Roda DENTRO da VM. Faz duas verificações que a medição principal deixou em aberto.
#
# A) O atraso do Chaos Mesh (TF-02 e TF-02b) chegou ao tráfego de verdade?
#    O worker fala com o RabbitMQ e com o Dispatcher pelo nome do Service (um IP virtual). O NetworkChaos
#    dos manifestos mira o IP dos pods. Se o filtro do Chaos Mesh só casa com o IP do pod, o atraso não
#    pega no tráfego que passa pelo Service. Aqui medimos o tempo de conexão TCP a partir de cada worker,
#    antes e durante o atraso, pelo Service e direto no IP do pod. Também testamos uma variante que mira
#    o IP do Service (externalTargets).
# B) O p95 alto do TC-01 da primeira rodada se repete? Duas rodadas seguidas, sem falha, com amostras de
#    CPU e memória dos pods e a checagem de alarmes do RabbitMQ.
#
# Quem chama é scripts/k3s-verificar.sh. Dura uns 6 a 8 minutos. Nada aqui é calculado à mão:
# os números são as saídas cruas dos comandos, gravadas em carga/resultados/$PASTA/.
set -uo pipefail
cd "$(dirname "$0")/../.." || exit 1
export KUBECONFIG=${KUBECONFIG:-$HOME/.kube/config}

PASTA=${PASTA:-k3s-verificacao}
SAIDA=carga/resultados/$PASTA
mkdir -p "$SAIDA"
NO=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
BASE=${BASE:-http://$NO}
K="kubectl -n apurador"
WORKERS="worker-0 worker-1 worker-2"

log() { printf '\n[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }

esperar_cluster_pronto() {
    local vivos=0 pend=-1
    for _ in $(seq 1 100); do
        vivos=$(curl -fsS "$BASE/api/cluster" 2>/dev/null \
            | python3 -c 'import sys,json; print(sum(1 for w in json.load(sys.stdin)["workers"] if w["estado"]=="VIVO"))' 2>/dev/null || echo 0)
        pend=$(curl -fsS "$BASE/api/consistencia" 2>/dev/null \
            | python3 -c 'import sys,json; print(json.load(sys.stdin)["pendentes"])' 2>/dev/null || echo -1)
        if [ "$vivos" = 3 ] && [ "$pend" = 0 ]; then return 0; fi
        sleep 3
    done
    echo "AVISO: o cluster não ficou pronto (workers vivos=$vivos, pendentes=$pend). Seguindo mesmo assim."
    return 1
}

# ---------------------------------------------------------------------------------------------
# Parte A
# ---------------------------------------------------------------------------------------------
# Milissegundos de uma conexão TCP aberta de dentro do pod $1 para $2:$3. Inclui o tempo de subir um
# bash filho (algumas dezenas de ms), o que não atrapalha: o atraso testado é de 300 ms ou 2500 ms.
conectar_ms() {
    # shellcheck disable=SC2016  # as aspas simples são de propósito: a expansão acontece dentro do pod
    $K exec "$1" -- bash -c 's=$(date +%s%N); if timeout 6 bash -c "echo > /dev/tcp/$0/$1" 2>/dev/null; then e=$(date +%s%N); echo $(( (e-s)/1000000 )); else echo erro; fi' "$2" "$3" 2>/dev/null || echo erro
}

medir_rodada() {   # $1 = rótulo. Uma linha por worker e alvo, com 3 amostras em ms.
    local rot=$1 w alvo nome host porta amostras
    for w in $WORKERS; do
        for alvo in "dispatcher-service dispatcher 8080" "dispatcher-ip $IP_D 8080" "rabbitmq-service rabbitmq 5672" "rabbitmq-ip $IP_R 5672"; do
            read -r nome host porta <<< "$alvo"
            amostras=""
            for _ in 1 2 3; do amostras="$amostras $(conectar_ms "$w" "$host" "$porta")"; done
            printf '%s; %s; %s; ms:%s\n' "$rot" "$w" "$nome" "$amostras"
        done
    done
}

# Maior silêncio de heartbeat que o Dispatcher viu de cada worker em 12 s, e os estados por que passaram.
silencio_do_heartbeat() {   # $1 = rótulo
    python3 - "$BASE" "$1" <<'PY'
import json, sys, time, urllib.request
base, rot = sys.argv[1], sys.argv[2]
maximo, estados = {}, {}
fim = time.time() + 12
while time.time() < fim:
    try:
        visao = json.load(urllib.request.urlopen(base + "/api/cluster", timeout=2))
    except Exception:
        time.sleep(0.3)
        continue
    for w in visao["workers"]:
        maximo[w["id"]] = max(maximo.get(w["id"], 0), w["silencioMs"])
        estados.setdefault(w["id"], set()).add(w["estado"])
    time.sleep(0.3)
print(rot + "; silêncio máximo do heartbeat (ms): " + ", ".join(
    "%s=%d %s" % (k, maximo[k], sorted(estados[k])) for k in sorted(maximo)))
PY
}

aplicar() {   # $1 = arquivo yaml
    kubectl apply -f "$1" >/dev/null
    sleep 6   # o Chaos Mesh leva alguns segundos para instalar a regra de tráfego em cada pod
}

remover() {   # $1 = arquivo yaml
    kubectl delete -f "$1" --ignore-not-found >/dev/null
    sleep 6
}

parte_a() {
    log "Parte A: o atraso do Chaos Mesh chega ao tráfego?"
    local tmp cip_d cip_r
    tmp=$(mktemp -d)
    IP_D=$($K get pod -l app=dispatcher -o jsonpath='{.items[0].status.podIP}')
    IP_R=$($K get pod -l app=rabbitmq -o jsonpath='{.items[0].status.podIP}')
    cip_d=$($K get svc dispatcher -o jsonpath='{.spec.clusterIP}')
    cip_r=$($K get svc rabbitmq -o jsonpath='{.spec.clusterIP}')
    {
        echo "IP do pod do dispatcher: $IP_D   IP do Service dispatcher: $cip_d"
        echo "IP do pod do rabbitmq:   $IP_R   IP do Service rabbitmq:   $cip_r"
        echo "Formato: rótulo; worker; alvo; ms de conexão TCP (3 amostras). 'erro' = não conectou em 6 s."
        echo
    } | tee "$SAIDA/atraso-medicao.txt"

    awk -v dir="$tmp" 'BEGIN{n=0} /^---[[:space:]]*$/ {n++; next} {print > (dir "/parte-" n ".yaml")}' chaos/atrasar-mensagens.yaml

    # Variantes que miram o IP do Service em vez do IP do pod.
    cat > "$tmp/variante-rabbit.yaml" <<EOF
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: verificar-rabbit-clusterip
  namespace: apurador
spec:
  action: delay
  mode: all
  selector:
    namespaces: [apurador]
    labelSelectors:
      app: worker
  direction: to
  externalTargets: ["$cip_r"]
  delay:
    latency: 300ms
    jitter: 100ms
    correlation: "25"
  duration: 60s
EOF
    cat > "$tmp/variante-heartbeat.yaml" <<EOF
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: verificar-heartbeat-clusterip
  namespace: apurador
spec:
  action: delay
  mode: one
  selector:
    namespaces: [apurador]
    labelSelectors:
      app: worker
  direction: to
  externalTargets: ["$cip_d"]
  delay:
    latency: 2500ms
    jitter: 1000ms
  duration: 60s
EOF

    {
        medir_rodada "1-sem-atraso"
        silencio_do_heartbeat "1-sem-atraso"

        echo "--- TF-02 como está no repositório (alvo: pods do rabbitmq)"
        aplicar "$tmp/parte-0.yaml"
        medir_rodada "2-tf02-yaml-atual"
        remover "$tmp/parte-0.yaml"

        echo "--- TF-02b como está no repositório (alvo: pods do dispatcher)"
        aplicar "$tmp/parte-1.yaml"
        medir_rodada "3-tf02b-yaml-atual"
        silencio_do_heartbeat "3-tf02b-yaml-atual"
        remover "$tmp/parte-1.yaml"

        echo "--- variante do TF-02 mirando o IP do Service do rabbitmq (externalTargets)"
        aplicar "$tmp/variante-rabbit.yaml"
        medir_rodada "4-tf02-variante-service"
        remover "$tmp/variante-rabbit.yaml"

        echo "--- variante do TF-02b mirando o IP do Service do dispatcher (externalTargets)"
        aplicar "$tmp/variante-heartbeat.yaml"
        medir_rodada "5-tf02b-variante-service"
        silencio_do_heartbeat "5-tf02b-variante-service"
        remover "$tmp/variante-heartbeat.yaml"

        echo "--- de novo sem atraso, para conferir que tudo voltou ao normal"
        medir_rodada "6-depois-sem-atraso"
    } 2>&1 | tee -a "$SAIDA/atraso-medicao.txt"
    kubectl get networkchaos -A 2>&1 | tee "$SAIDA/networkchaos-restantes.txt"
    rm -rf "$tmp"
}

# ---------------------------------------------------------------------------------------------
# Parte B
# ---------------------------------------------------------------------------------------------
parte_b() {
    log "Parte B: o TC-01 sem falha, duas vezes seguidas"
    local n amostrador
    ( while true; do echo "--- $(date +%H:%M:%S)"; kubectl -n apurador top pods --no-headers 2>&1; sleep 5; done ) > "$SAIDA/recursos-durante-tc01.txt" &
    amostrador=$!
    : > "$SAIDA/rabbitmq-alarmes.txt"
    for n in 1 2; do
        esperar_cluster_pronto
        k6 run -e BASE="$BASE" -e RODADA="k3s-rep$n" -e TAXA=40 -e CHAVES=30 -e DURACAO=60s \
            -e SAIDA="$SAIDA" carga/rajada-resultados.js > "$SAIDA/rajada-k3s-rep$n.log" 2>&1
        echo "--- repetição $n"; cat "$SAIDA/rajada-k3s-rep$n.txt"
        { echo "--- depois da repetição $n ($(date +%H:%M:%S))"; $K exec rabbitmq-0 -- rabbitmq-diagnostics check_local_alarms 2>&1; } >> "$SAIDA/rabbitmq-alarmes.txt"
    done
    kill "$amostrador" 2>/dev/null || true
    wait "$amostrador" 2>/dev/null || true
    curl -fsS "$BASE/api/consistencia" | tee "$SAIDA/consistencia-depois.json"; echo
}

log "Ambiente"
{
    echo "Data: $(date -Is)"
    echo "VM: $(uname -srm), $(nproc) CPUs, $(free -m | awk '/^Mem:/ {print $2}') MB"
    echo "Chaos Mesh: $(helm list -n chaos-mesh -o json 2>/dev/null)"
    kubectl -n apurador get pods -o wide
} > "$SAIDA/ambiente.txt" 2>&1
cat "$SAIDA/ambiente.txt"

esperar_cluster_pronto
parte_a
parte_b

tar czf "$HOME/verificacao-k3s.tgz" -C carga/resultados "$PASTA"
log "Fim. Saídas em $SAIDA e em $HOME/verificacao-k3s.tgz"
