#!/bin/bash
# TF-03: derruba o Gateway/Dispatcher durante a rajada e mede quanto tempo ele leva para voltar.
# Rodar enquanto o TC-01 está rodando. O TC-01 vai registrar erros HTTP nesse intervalo:
# isso é esperado, porque o Gateway é ponto único de falha (limitação assumida).
# O que se verifica é que nenhum evento ACEITO (202) se perde.
#
# Sobre o tempo de volta: com --force o pod antigo some do Kubernetes na hora, mas o processo dele
# pode continuar atendendo por um instante. Na medição k3s-tf03 o primeiro curl bem-sucedido veio em
# 243 ms, enquanto o observador viu o Gateway mudo por mais de 6 s. A leitura mais provável é que aquele
# curl tenha sido respondido pelo processo antigo (não foi confirmado). Por isso este script só dá o tempo
# como "de volta" depois que existe um pod NOVO pronto.
set -euo pipefail
BASE=${BASE:-http://localhost}
inicio=$(date +%s%3N)
kubectl -n apurador delete pod -l app=dispatcher --grace-period=0 --force
until kubectl -n apurador get pod -l app=dispatcher -o jsonpath='{range .items[*]}{.status.containerStatuses[0].ready}{"\n"}{end}' 2>/dev/null | grep -q true; do sleep 0.2; done
pronto=$(date +%s%3N)
until curl -fsS -o /dev/null "$BASE/api/cluster"; do sleep 0.2; done
fim=$(date +%s%3N)
echo "Pod novo do Gateway pronto depois de $((pronto - inicio)) ms"
echo "Gateway respondendo de novo depois de $((fim - inicio)) ms"
