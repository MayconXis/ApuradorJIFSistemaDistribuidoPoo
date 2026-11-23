#!/bin/bash
# Repete só algumas etapas da medição no k3s da VM, sem refazer tudo e sem tocar em carga/resultados/k3s/.
# Serve para refazer o que a primeira medição deixou inválido ou em dúvida (ver carga/resultados/k3s/LEIA-ME.txt).
#
# Uso, a partir da raiz do repositório (a pasta apurador-jif), com a VM ligada:
#   bash scripts/k3s-repetir.sh tf02 tf02b tf03
#
# Etapas aceitas: tc01 tc02 tf01 tf02 tf02b tf03 (uma ou mais, na ordem em que devem rodar).
# As saídas vão para carga/resultados/k3s-repeticao-<etapas>/ (ex.: k3s-repeticao-tf02-tf02b-tf03). Se essa pasta já
# existe, a nova leva o sufixo -2, -3, e assim por diante, e a anterior não é tocada.
#
# Não precisa do Docker Desktop. tf02 + tf02b + tf03 levam uns 8 minutos. Como no `medir`, não deixe o Mac
# dormir (caffeinate -dimsu em outra aba) e não mexa no painel do Cluster enquanto roda.
# Variáveis opcionais: VM_NOME (apurador), PASTA (nome da subpasta de carga/resultados).
# Compatível com o bash 3.2 que vem no macOS.
set -euo pipefail
cd "$(dirname "$0")/.."

VM=${VM_NOME:-apurador}
TMP=_k3s
VM_HOME=/home/ubuntu

if [ "$#" -eq 0 ]; then
    echo "Diga quais etapas repetir. Exemplo:  bash scripts/k3s-repetir.sh tf02 tf02b tf03"
    echo "Etapas aceitas: tc01 tc02 tf01 tf02 tf02b tf03"
    exit 1
fi
for e in "$@"; do
    case "$e" in
        tc01|tc02|tf01|tf02|tf02b|tf03) ;;
        *) echo "Etapa desconhecida: $e. Aceitas: tc01 tc02 tf01 tf02 tf02b tf03"; exit 1 ;;
    esac
done
ETAPAS="$*"
SUFIXO=$(echo "$ETAPAS" | tr ' ' '-')
PASTA_BASE=${PASTA:-k3s-repeticao-$SUFIXO}
# Uma repetição nunca sobrescreve a anterior: se a pasta já existe, a nova ganha -2, -3, ...
PASTA=$PASTA_BASE
N=2
while [ -e "carga/resultados/$PASTA" ]; do
    PASTA="$PASTA_BASE-$N"
    N=$((N + 1))
done

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

echo "== Repetindo: $ETAPAS (saídas em carga/resultados/$PASTA/) =="
multipass exec "$VM" -- env PASTA="$PASTA" ETAPAS="$ETAPAS" bash "$VM_HOME/apurador-jif/scripts/vm/medir-no-k3s.sh"

multipass transfer "$VM:$VM_HOME/resultados-$PASTA.tgz" "$TMP/resultados-$PASTA.tgz"
tar xzf "$TMP/resultados-$PASTA.tgz" -C carga/resultados
if [ -f "$TMP/anfitriao.txt" ]; then
    cp "$TMP/anfitriao.txt" "carga/resultados/$PASTA/anfitriao.txt"
fi
echo
echo "Saídas copiadas para carga/resultados/$PASTA/. Pode avisar que terminou."
