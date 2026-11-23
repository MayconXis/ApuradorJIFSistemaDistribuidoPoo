#!/bin/bash
# Duas verificações no k3s da VM, para fechar dúvidas deixadas pela medição principal (`medir`):
#   A) o atraso do Chaos Mesh (TF-02 e TF-02b) chegou ao tráfego de verdade?
#   B) o p95 alto do TC-01 da primeira rodada se repete?
# Os resultados vão para carga/resultados/k3s-verificacao/, sem tocar nos de carga/resultados/k3s/.
#
# Uso, a partir da raiz do repositório (a pasta apurador-jif), com a VM ligada:
#   bash scripts/k3s-verificar.sh
#
# Não precisa do Docker Desktop. Dura uns 6 a 8 minutos. Como no `medir`, não deixe o Mac dormir
# (caffeinate -dimsu em outra aba) e não mexa no painel do Cluster enquanto roda.
# Variável opcional: VM_NOME (apurador). Compatível com o bash 3.2 que vem no macOS.
set -euo pipefail
cd "$(dirname "$0")/.."

VM=${VM_NOME:-apurador}
TMP=_k3s
VM_HOME=/home/ubuntu
PASTA=k3s-verificacao

if ! command -v multipass >/dev/null 2>&1; then
    echo "O Multipass não foi encontrado. Este script só funciona depois do 'bash scripts/k3s-no-mac.sh criar'."
    exit 1
fi
if ! multipass info "$VM" >/dev/null 2>&1; then
    echo "A VM '$VM' não existe. Rode antes:  bash scripts/k3s-no-mac.sh criar"
    exit 1
fi
multipass start "$VM" >/dev/null 2>&1 || true

echo "== Código atualizado para a VM =="
mkdir -p "$TMP" "carga/resultados/$PASTA"
# Mesmos itens excluídos do k3s-no-mac.sh. Os resultados que já estão na VM não são tocados.
COPYFILE_DISABLE=1 tar czf "$TMP/repo.tgz" \
    --exclude=_k3s --exclude=_maven-offline --exclude=_versao-anterior \
    --exclude=node_modules --exclude=target --exclude=.git --exclude=.angular --exclude=dist \
    --exclude=docs --exclude=resultados .
multipass transfer "$TMP/repo.tgz" "$VM:$VM_HOME/repo.tgz"
# shellcheck disable=SC2016  # o $HOME é da VM, não do Mac
multipass exec "$VM" -- bash -c 'mkdir -p "$HOME/apurador-jif" && tar xzf "$HOME/repo.tgz" -C "$HOME/apurador-jif"'

echo "== Verificações (A: atraso do Chaos Mesh, B: TC-01 duas vezes) =="
multipass exec "$VM" -- env PASTA="$PASTA" bash "$VM_HOME/apurador-jif/scripts/vm/verificar-na-vm.sh"

multipass transfer "$VM:$VM_HOME/verificacao-k3s.tgz" "$TMP/verificacao-k3s.tgz"
tar xzf "$TMP/verificacao-k3s.tgz" -C carga/resultados
echo
echo "Saídas copiadas para carga/resultados/$PASTA/. Pode avisar que terminou."
