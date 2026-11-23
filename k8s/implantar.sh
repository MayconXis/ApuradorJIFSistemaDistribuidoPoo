#!/bin/bash
# Implanta o Apurador JIF em um k3s que já está rodando neste nó.
# Uso, a partir da raiz do repositório:  bash k8s/implantar.sh
# Com IMAGENS_JA_NO_K3S=1 ele pula o build e a importação (usado por scripts/k3s-no-mac.sh).
set -euo pipefail
cd "$(dirname "$0")/.."
VERSAO=1.0.0

if [ "${IMAGENS_JA_NO_K3S:-0}" != 1 ]; then
    echo "== 1. Imagens =="
    docker build -f docker/Dockerfile.java --build-arg MODULO=dispatcher -t apurador/dispatcher:$VERSAO .
    docker build -f docker/Dockerfile.java --build-arg MODULO=worker -t apurador/worker:$VERSAO .
    docker build -f docker/Dockerfile.frontend -t apurador/frontend:$VERSAO .

    echo "== 2. Imagens para dentro do k3s (o k3s usa o containerd, não o Docker) =="
    for img in dispatcher worker frontend; do
        docker save apurador/$img:$VERSAO | sudo k3s ctr images import -
    done
else
    echo "== 1 e 2. Imagens já importadas no k3s (IMAGENS_JA_NO_K3S=1), pulando o build =="
fi

echo "== 3. Manifestos =="
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configuracao.yaml
kubectl -n apurador create configmap postgres-esquema \
    --from-file=01-schema.sql=persistencia/src/main/resources/db/schema.sql \
    --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/02-postgres.yaml -f k8s/03-rabbitmq.yaml
kubectl -n apurador rollout status statefulset/postgres --timeout=180s
kubectl -n apurador rollout status statefulset/rabbitmq --timeout=240s
kubectl apply -f k8s/04-dispatcher.yaml
kubectl -n apurador rollout status deployment/dispatcher --timeout=240s
kubectl apply -f k8s/05-worker.yaml -f k8s/06-frontend.yaml -f k8s/07-ingress.yaml
kubectl -n apurador rollout status statefulset/worker --timeout=300s
kubectl -n apurador rollout status deployment/frontend --timeout=120s

echo "== 4. Pronto =="
kubectl -n apurador get pods -o wide
IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
echo "Interface: http://$IP/"
