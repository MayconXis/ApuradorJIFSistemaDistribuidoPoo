#!/bin/bash
# Troca só a interface (imagem do frontend) no k3s da VM, sem mexer em Dispatcher, workers, RabbitMQ nem PostgreSQL.
# Serve para levar para o k3s uma mudança que foi só de tela (HTML, CSS, logo), sem derrubar o cluster.
#
# Uso, a partir da raiz do repositório (a pasta apurador-jif), com a VM já criada:
#   bash scripts/k3s-atualizar-frontend.sh
#
# O Docker Desktop precisa estar aberto, porque ele gera a imagem. Se o `medir` ainda está rodando, espere acabar.
# Variável opcional: VM_NOME (apurador). Compatível com o bash 3.2 que vem no macOS.
set -euo pipefail
cd "$(dirname "$0")/.."

VM=${VM_NOME:-apurador}
VERSAO=1.0.0
TMP=_k3s
VM_HOME=/home/ubuntu

if ! command -v multipass >/dev/null 2>&1; then
    echo "O Multipass não foi encontrado. Este script só funciona depois do 'bash scripts/k3s-no-mac.sh criar'."
    exit 1
fi
if ! multipass info "$VM" >/dev/null 2>&1; then
    echo "A VM '$VM' não existe. Rode antes:  bash scripts/k3s-no-mac.sh criar"
    exit 1
fi
if ! command -v docker >/dev/null 2>&1 || ! docker info >/dev/null 2>&1; then
    echo "O Docker Desktop não está rodando. Abra o aplicativo, espere ele ficar verde e rode de novo."
    exit 1
fi

echo "== Imagem da interface (Docker Desktop) =="
mkdir -p "$TMP"
PROV=""
if docker build --help 2>/dev/null | grep -q -- '--provenance'; then
    # Mesmo motivo do k3s-no-mac.sh: sem o atestado extra, o containerd do k3s importa sem reclamar.
    PROV="--provenance=false"
fi
# shellcheck disable=SC2086
docker build $PROV -f docker/Dockerfile.frontend -t apurador/frontend:$VERSAO .
docker save apurador/frontend:$VERSAO -o "$TMP/frontend.tar"
ls -lh "$TMP/frontend.tar"

echo "== Enviando para a VM e importando no k3s =="
multipass transfer "$TMP/frontend.tar" "$VM:$VM_HOME/frontend.tar"
multipass exec "$VM" -- sudo k3s ctr images import "$VM_HOME/frontend.tar"

echo "== Reiniciando só o frontend =="
multipass exec "$VM" -- sudo k3s kubectl -n apurador rollout restart deployment/frontend
multipass exec "$VM" -- sudo k3s kubectl -n apurador rollout status deployment/frontend --timeout=120s

IP=$(multipass info "$VM" | awk '/^IPv4/ {print $2; exit}')
echo
echo "Pronto. Abra http://$IP/ e force a atualização da página (Cmd+Shift+R) para não pegar o CSS antigo do navegador."
echo "Os workers não foram reiniciados."
