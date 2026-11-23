#!/bin/bash
# Roda DENTRO da VM. Executa TC-01, TC-02, TF-01, TF-02 (com a variante 02b) e TF-03 contra o k3s
# e guarda as saídas em carga/resultados/<pasta>/. Os números vêm do k6 e do observador do cluster,
# nada é calculado à mão. O k6 roda na própria VM (limitação: ele divide a CPU com o cluster).
#
# Os parâmetros são os mesmos das medições fora do k3s, para a comparação ser justa:
#   40 resultados/s por 60 s em 30 chaves, 200 usuários virtuais na consulta.
# Quem chama é scripts/k3s-no-mac.sh medir (tudo, uns 20 a 30 minutos) ou scripts/k3s-repetir.sh (só
# algumas etapas, em outra pasta). Variáveis:
#   ETAPAS  quais etapas rodar, separadas por espaço: tc01 tc02 tf01 tf02 tf02b tf03 (padrão: todas)
#   PASTA   subpasta de carga/resultados para as saídas (padrão: k3s)
set -uo pipefail
cd "$(dirname "$0")/../.." || exit 1
export KUBECONFIG=${KUBECONFIG:-$HOME/.kube/config}

NO=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
BASE=${BASE:-http://$NO}
PASTA=${PASTA:-k3s}
SAIDA=carga/resultados/$PASTA
ETAPAS=${ETAPAS:-tc01 tc02 tf01 tf02 tf02b tf03}
TAXA=${TAXA:-40}
CHAVES=${CHAVES:-30}
DURACAO=${DURACAO:-60s}
# Identificadores dos eventos e das chaves da carga levam este sufixo, que muda a cada execução do script.
# Sem ele, repetir uma etapa reaproveitaria os identificadores da execução anterior: os eventos já existem no
# banco, voltam como repetidos e a rajada quase não aplica nada (foi o que aconteceu na primeira repetição).
ID_EXTRA=${ID_EXTRA:-$(date +%d%H%M%S)}
mkdir -p "$SAIDA"
TMPCHAOS=$(mktemp -d)

log() { printf '\n[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }

quer() {   # $1 = etapa. Sucesso se ela está em $ETAPAS.
    case " $ETAPAS " in *" $1 "*) return 0 ;; esac
    return 1
}

# Espera os 3 workers VIVOS e nenhum evento pendente (até 5 minutos).
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

rajada() {   # $1 = nome da rodada (vira o prefixo dos arquivos)
    k6 run -e BASE="$BASE" -e RODADA="$1" -e TAXA="$TAXA" -e CHAVES="$CHAVES" -e DURACAO="$DURACAO" \
        -e ID_EXTRA="$ID_EXTRA" -e SAIDA="$SAIDA" carga/rajada-resultados.js
}

observar() {   # $1 = arquivo de saída; roda em segundo plano
    python3 chaos/observar_cluster.py --base "$BASE" --intervalo 0.2 --duracao 400 > "$1" 2>&1 &
    OBS=$!
}

parar_observador() {
    kill "$OBS" 2>/dev/null || true
    wait "$OBS" 2>/dev/null || true
}

estado() {   # $1 = rótulo
    kubectl -n apurador get pods -o wide > "$SAIDA/pods-$1.txt" 2>&1
    kubectl -n apurador get events --sort-by=.lastTimestamp > "$SAIDA/eventos-$1.txt" 2>&1
}

# Milissegundos de uma conexão TCP aberta de dentro do pod $1 para $2:$3, pelo nome do Service.
# É a prova de que o atraso do Chaos Mesh chegou ao tráfego (o describe só mostra que ele foi "injetado").
conectar_ms() {
    # shellcheck disable=SC2016  # as aspas simples são de propósito: a expansão acontece dentro do pod
    kubectl -n apurador exec "$1" -- bash -c 's=$(date +%s%N); if timeout 6 bash -c "echo > /dev/tcp/$0/$1" 2>/dev/null; then e=$(date +%s%N); echo $(( (e-s)/1000000 )); else echo erro; fi' "$2" "$3" 2>/dev/null || echo erro
}

provar_atraso() {   # $1 = arquivo de saída, $2 = host, $3 = porta. 3 amostras de cada worker.
    local w amostras
    {
        echo "ms de conexão TCP até $2:$3 (pelo Service), a partir de cada worker, com o atraso aplicado"
        for w in worker-0 worker-1 worker-2; do
            amostras=""
            for _ in 1 2 3; do amostras="$amostras $(conectar_ms "$w" "$2" "$3")"; done
            echo "$w: $amostras"
        done
    } > "$1"
}

# Os atrasos miram o IP do Service, não o do pod. Ver o comentário de chaos/atrasar-mensagens.yaml.
gerar_atrasos() {
    local cip_r cip_d
    cip_r=$(kubectl -n apurador get svc rabbitmq -o jsonpath='{.spec.clusterIP}')
    cip_d=$(kubectl -n apurador get svc dispatcher -o jsonpath='{.spec.clusterIP}')
    sed -e "s/__IP_SERVICE_RABBITMQ__/$cip_r/" -e "s/__IP_SERVICE_DISPATCHER__/$cip_d/" chaos/atrasar-mensagens.yaml \
        | awk -v dir="$TMPCHAOS" 'BEGIN{n=0} /^---[[:space:]]*$/ {n++; next} {print > (dir "/parte-" n ".yaml")}'
}

log "Ambiente (etapas: $ETAPAS; pasta: $PASTA)"
{
    echo "Data: $(date -Is)"
    echo "Etapas: $ETAPAS"
    echo "VM: $(uname -srm), $(nproc) CPUs, $(free -m | awk '/^Mem:/ {print $2}') MB"
    echo "k3s: $(k3s --version | head -1)"
    echo "k6: $(k6 version)"
    echo "Chaos Mesh: $(helm list -n chaos-mesh -o json 2>/dev/null)"
    echo "Base das requisições: $BASE (pelo Traefik e pelo nginx do frontend)"
    echo "Parâmetros: taxa=$TAXA/s duração=$DURACAO chaves=$CHAVES sufixo dos identificadores=$ID_EXTRA"
    kubectl get nodes -o wide
    kubectl -n apurador get pods -o wide
    kubectl -n apurador get configmap apurador-config -o yaml
} > "$SAIDA/ambiente.txt" 2>&1

# ---------------------------------------------------------------------------------------------
if quer tc01; then
    log "TC-01 rajada de resultados, sem falha"
    esperar_cluster_pronto
    rajada k3s-tc01 2>&1 | tee "$SAIDA/rajada-k3s-tc01.log"
fi

if quer tc02; then
    log "TC-02 consultas simultâneas"
    esperar_cluster_pronto
    k6 run -e BASE="$BASE" -e USUARIOS=200 -e SAIDA="$SAIDA" carga/consultas-simultaneas.js 2>&1 | tee "$SAIDA/consultas-k3s-tc02.log"
fi

# ---------------------------------------------------------------------------------------------
if quer tf01; then
    log "TF-01 mata um worker no meio da rajada (Chaos Mesh PodChaos)"
    esperar_cluster_pronto
    estado tf01-antes
    observar "$SAIDA/observador-tf01.txt"
    rajada k3s-tf01 > "$SAIDA/rajada-k3s-tf01.log" 2>&1 &
    K6=$!
    sleep 25
    echo "PodChaos aplicado em $(date +%s.%N) (epoch, mesma escala do observador)" > "$SAIDA/tf01-instante-da-falha.txt"
    kubectl apply -f chaos/matar-worker.yaml | tee -a "$SAIDA/tf01-instante-da-falha.txt"
    wait "$K6"
    kubectl delete -f chaos/matar-worker.yaml
    esperar_cluster_pronto
    parar_observador
    estado tf01-depois
    cat "$SAIDA/rajada-k3s-tf01.log"
    echo "--- observador TF-01 ---"; cat "$SAIDA/observador-tf01.txt"
fi

# ---------------------------------------------------------------------------------------------
if quer tf02 || quer tf02b; then
    gerar_atrasos
fi

if quer tf02; then
    log "TF-02 atrasa 300 ms (jitter 100 ms) entre os workers e o RabbitMQ (IP do Service) por 60 s"
    esperar_cluster_pronto
    estado tf02-antes
    observar "$SAIDA/observador-tf02.txt"
    rajada k3s-tf02 > "$SAIDA/rajada-k3s-tf02.log" 2>&1 &
    K6=$!
    sleep 3
    echo "NetworkChaos aplicado em $(date +%s.%N)" > "$SAIDA/tf02-instante-da-falha.txt"
    kubectl apply -f "$TMPCHAOS/parte-0.yaml" | tee -a "$SAIDA/tf02-instante-da-falha.txt"
    sleep 5
    # Prova de que o atraso chegou ao tráfego: sem isso o resultado não vale nada.
    kubectl -n apurador describe networkchaos atrasar-mensagens > "$SAIDA/tf02-networkchaos-describe.txt" 2>&1
    provar_atraso "$SAIDA/tf02-prova-do-atraso.txt" rabbitmq 5672
    wait "$K6"
    kubectl delete -f "$TMPCHAOS/parte-0.yaml" --ignore-not-found
    esperar_cluster_pronto
    parar_observador
    estado tf02-depois
    cat "$SAIDA/rajada-k3s-tf02.log"
    cat "$SAIDA/tf02-prova-do-atraso.txt"
    echo "--- observador TF-02 (cada linha 'saiu de VIVO' é um falso positivo ou uma suspeita) ---"
    cat "$SAIDA/observador-tf02.txt"
fi

if quer tf02b; then
    log "TF-02b atrasa o heartbeat de um worker até o Dispatcher (2,5 s com jitter de 1 s, IP do Service) por 60 s"
    esperar_cluster_pronto
    estado tf02b-antes
    observar "$SAIDA/observador-tf02b.txt"
    rajada k3s-tf02b > "$SAIDA/rajada-k3s-tf02b.log" 2>&1 &
    K6=$!
    sleep 3
    echo "NetworkChaos aplicado em $(date +%s.%N)" > "$SAIDA/tf02b-instante-da-falha.txt"
    kubectl apply -f "$TMPCHAOS/parte-1.yaml" | tee -a "$SAIDA/tf02b-instante-da-falha.txt"
    sleep 5
    kubectl -n apurador describe networkchaos atrasar-heartbeat > "$SAIDA/tf02b-networkchaos-describe.txt" 2>&1
    provar_atraso "$SAIDA/tf02b-prova-do-atraso.txt" dispatcher 8080
    wait "$K6"
    kubectl delete -f "$TMPCHAOS/parte-1.yaml" --ignore-not-found
    esperar_cluster_pronto
    parar_observador
    estado tf02b-depois
    cat "$SAIDA/rajada-k3s-tf02b.log"
    cat "$SAIDA/tf02b-prova-do-atraso.txt"
    echo "--- observador TF-02b ---"; cat "$SAIDA/observador-tf02b.txt"
fi

# ---------------------------------------------------------------------------------------------
if quer tf03; then
    log "TF-03 derruba o Gateway/Dispatcher no meio da rajada"
    esperar_cluster_pronto
    estado tf03-antes
    observar "$SAIDA/observador-tf03.txt"
    rajada k3s-tf03 > "$SAIDA/rajada-k3s-tf03.log" 2>&1 &
    K6=$!
    sleep 20
    BASE="$BASE" bash chaos/derrubar-gateway.sh 2>&1 | tee "$SAIDA/tf03-tempo-de-volta.txt"
    wait "$K6"
    esperar_cluster_pronto
    parar_observador
    estado tf03-depois
    cat "$SAIDA/rajada-k3s-tf03.log"
    echo "--- observador TF-03 ---"; cat "$SAIDA/observador-tf03.txt"
fi

# ---------------------------------------------------------------------------------------------
log "Consistência acumulada no fim de tudo"
curl -fsS "$BASE/api/consistencia" | tee "$SAIDA/consistencia-final.json"
echo

rm -rf "$TMPCHAOS"
tar czf "$HOME/resultados-$PASTA.tgz" -C carga/resultados "$PASTA"
log "Fim. Saídas em $SAIDA e no arquivo $HOME/resultados-$PASTA.tgz"
