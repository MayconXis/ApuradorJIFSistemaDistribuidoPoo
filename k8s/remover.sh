#!/bin/bash
# Remove tudo o que o implantar.sh criou, inclusive os dados do banco e das filas.
set -euo pipefail
kubectl delete namespace apurador --wait=true
