#!/bin/bash
# Cria, no Mac, uma VM Ubuntu com o Multipass, instala o k3s dentro dela e implanta o Apurador JIF.
# O Docker Desktop só gera as imagens. Quem roda o sistema é o k3s da VM.
#
# Uso, a partir da raiz do repositório (a pasta apurador-jif):
#   bash scripts/k3s-no-mac.sh criar       cria a VM, instala o k3s, implanta o sistema e instala o Chaos Mesh (padrão)
#   bash scripts/k3s-no-mac.sh medir       roda TC-01, TC-02, TF-01, TF-02, TF-03 dentro da VM e traz as saídas
#   bash scripts/k3s-no-mac.sh ip          mostra o endereço da interface
#   bash scripts/k3s-no-mac.sh pods        mostra os pods
#   bash scripts/k3s-no-mac.sh atualizar   gera as imagens de novo e reinicia o sistema (depois de mudar código)
#   bash scripts/k3s-no-mac.sh parar       desliga a VM, sem apagar nada
#   bash scripts/k3s-no-mac.sh apagar      apaga a VM
#
# Variáveis opcionais: VM_NOME (apurador), VM_CPUS (4), VM_MEMORIA (8G), VM_DISCO (40G).
# Compatível com o bash 3.2 que vem no macOS.
set -euo pipefail
cd "$(dirname "$0")/.."

VM=${VM_NOME:-apurador}
CPUS=${VM_CPUS:-4}
MEMORIA=${VM_MEMORIA:-8G}
DISCO=${VM_DISCO:-40G}
VERSAO=1.0.0
TMP=_k3s
VM_HOME=/home/ubuntu

verificar_ferramentas() {
    if ! command -v multipass >/dev/null 2>&1; then
        cat <<'EOF'
O Multipass não está instalado. Ele cria a VM Linux onde o k3s roda. Instale de um destes jeitos
(os dois pedem a senha do Mac) e rode este script de novo:
  brew install --cask multipass
  ou baixe o instalador em https://canonical.com/multipass/install
EOF
        exit 1
    fi
    if ! command -v docker >/dev/null 2>&1; then
        echo "O comando docker não foi encontrado. Instale o Docker Desktop e abra o aplicativo."
        exit 1
    fi
    if ! docker info >/dev/null 2>&1; then
        echo "O Docker Desktop não está rodando. Abra o aplicativo, espere ele ficar verde e rode de novo."
        exit 1
    fi
}

construir_imagens() {
    echo "== Imagens (Docker Desktop) =="
    mkdir -p "$TMP"
    PROV=""
    if docker build --help 2>/dev/null | grep -q -- '--provenance'; then
        # Sem o atestado extra, o docker save sai com um manifesto só e o containerd do k3s importa sem reclamar.
        PROV="--provenance=false"
    fi
    # shellcheck disable=SC2086
    docker build $PROV -f docker/Dockerfile.java --build-arg MODULO=dispatcher -t apurador/dispatcher:$VERSAO .
    # shellcheck disable=SC2086
    docker build $PROV -f docker/Dockerfile.java --build-arg MODULO=worker -t apurador/worker:$VERSAO .
    # shellcheck disable=SC2086
    docker build $PROV -f docker/Dockerfile.frontend -t apurador/frontend:$VERSAO .
    docker save apurador/dispatcher:$VERSAO apurador/worker:$VERSAO apurador/frontend:$VERSAO -o "$TMP/imagens.tar"
    ls -lh "$TMP/imagens.tar"
}

empacotar_repositorio() {
    echo "== Código para a VM =="
    mkdir -p "$TMP"
    COPYFILE_DISABLE=1 tar czf "$TMP/repo.tgz" \
        --exclude=_k3s --exclude=_maven-offline --exclude=_versao-anterior \
        --exclude=node_modules --exclude=target --exclude=.git --exclude=.angular --exclude=dist \
        --exclude=docs --exclude=resultados .
    ls -lh "$TMP/repo.tgz"
}

criar_vm() {
    echo "== VM Ubuntu no Multipass ($CPUS CPUs, $MEMORIA de memória, $DISCO de disco) =="
    if multipass info "$VM" >/dev/null 2>&1; then
        echo "A VM $VM já existe. Vou usar ela."
        multipass start "$VM" >/dev/null 2>&1 || true
    else
        multipass launch 24.04 --name "$VM" --cpus "$CPUS" --memory "$MEMORIA" --disk "$DISCO"
    fi
}

enviar_para_a_vm() {
    echo "== Enviando para a VM =="
    multipass transfer "$TMP/repo.tgz" "$VM:$VM_HOME/repo.tgz"
    multipass transfer "$TMP/imagens.tar" "$VM:$VM_HOME/imagens.tar"
    multipass exec "$VM" -- bash -c 'mkdir -p "$HOME/apurador-jif" && tar xzf "$HOME/repo.tgz" -C "$HOME/apurador-jif"'
}

endereco() {
    multipass info "$VM" | awk '/^IPv4/ {print $2; exit}'
}

cmd_criar() {
    verificar_ferramentas
    construir_imagens
    empacotar_repositorio
    criar_vm
    enviar_para_a_vm
    multipass exec "$VM" -- bash "$VM_HOME/apurador-jif/scripts/vm/preparar-vm.sh"
    IP=$(endereco)
    echo
    echo "Pronto. Interface no navegador do Mac: http://$IP/"
    echo "Para a demonstração, crie as chaves de exemplo com:  BASE=http://$IP bash scripts/demo/criar-chaves-demo.sh"
    echo "Para matar um worker na frente da plateia:  multipass exec $VM -- sudo k3s kubectl -n apurador delete pod worker-1 --grace-period=0 --force"
    echo "Dica: o Docker Desktop já cumpriu o papel. Pode fechar o aplicativo para liberar memória antes de medir."
}

cmd_medir() {
    mkdir -p "$TMP" carga/resultados/k3s
    {
        echo "Anfitrião: $(sw_vers -productName 2>/dev/null || uname -s) $(sw_vers -productVersion 2>/dev/null || true), $(uname -m)"
        echo "Memória do Mac (bytes): $(sysctl -n hw.memsize 2>/dev/null || echo desconhecida)"
        echo "Núcleos do Mac: $(sysctl -n hw.ncpu 2>/dev/null || echo desconhecido)"
        echo "Multipass: $(multipass version | head -1)"
        echo "VM: $VM, $CPUS CPUs, $MEMORIA de memória"
    } > "$TMP/anfitriao.txt"
    multipass exec "$VM" -- bash "$VM_HOME/apurador-jif/scripts/vm/medir-no-k3s.sh"
    multipass transfer "$VM:$VM_HOME/resultados-k3s.tgz" "$TMP/resultados-k3s.tgz"
    tar xzf "$TMP/resultados-k3s.tgz" -C carga/resultados
    cp "$TMP/anfitriao.txt" carga/resultados/k3s/anfitriao.txt
    echo "Saídas copiadas para carga/resultados/k3s/ (a pasta está dentro do repositório)."
}

cmd_atualizar() {
    verificar_ferramentas
    construir_imagens
    empacotar_repositorio
    enviar_para_a_vm
    multipass exec "$VM" -- bash "$VM_HOME/apurador-jif/scripts/vm/preparar-vm.sh"
    multipass exec "$VM" -- sudo k3s kubectl -n apurador rollout restart deployment/dispatcher statefulset/worker deployment/frontend
}

ACAO=${1:-criar}
case "$ACAO" in
    criar) cmd_criar ;;
    medir) cmd_medir ;;
    ip) echo "http://$(endereco)/" ;;
    pods) multipass exec "$VM" -- sudo k3s kubectl -n apurador get pods -o wide ;;
    atualizar) cmd_atualizar ;;
    parar) multipass stop "$VM" ;;
    apagar) multipass delete --purge "$VM" ;;
    *) echo "Ação desconhecida: $ACAO. Veja o cabeçalho deste arquivo."; exit 1 ;;
esac
