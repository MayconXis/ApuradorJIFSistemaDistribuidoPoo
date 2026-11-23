#!/bin/bash
# Instala o Chaos Mesh no k3s. O k3s usa o containerd em um caminho próprio,
# por isso o runtime e o socket precisam ser informados.
set -euo pipefail
helm repo add chaos-mesh https://charts.chaos-mesh.org
helm repo update
kubectl create namespace chaos-mesh --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install chaos-mesh chaos-mesh/chaos-mesh -n chaos-mesh \
    --set chaosDaemon.runtime=containerd \
    --set chaosDaemon.socketPath=/run/k3s/containerd/containerd.sock \
    --set dashboard.create=false
kubectl -n chaos-mesh rollout status deployment/chaos-controller-manager --timeout=300s
