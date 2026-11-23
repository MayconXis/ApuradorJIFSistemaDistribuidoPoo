#!/bin/bash
# Roda DENTRO da VM Ubuntu criada pelo Multipass. Instala o k3s, importa as imagens, implanta o
# Apurador JIF, instala o k6 e o Chaos Mesh. Quem chama é scripts/k3s-no-mac.sh, no Mac.
# Pode ser rodado de novo sem estragar nada.
set -euo pipefail
cd "$(dirname "$0")/../.."
export KUBECONFIG=$HOME/.kube/config

echo "== 1. k3s (já vem com o Traefik, que faz o papel de Ingress) =="
if ! command -v k3s >/dev/null 2>&1; then
    curl -sfL https://get.k3s.io | sh -
fi
mkdir -p "$HOME/.kube"
sudo cp /etc/rancher/k3s/k3s.yaml "$KUBECONFIG"
sudo chown "$USER" "$KUBECONFIG"
grep -q 'kube/config' "$HOME/.bashrc" || echo 'export KUBECONFIG=$HOME/.kube/config' >> "$HOME/.bashrc"
for _ in $(seq 1 60); do
    if kubectl get nodes 2>/dev/null | grep -qw Ready; then break; fi
    sleep 3
done
kubectl get nodes | grep -qw Ready || { echo "ERRO: o nó do k3s não ficou Ready em 3 minutos."; exit 1; }
kubectl get nodes -o wide

echo "== 2. Módulos do kernel que o NetworkChaos usa (atraso de rede) =="
# O kernel das imagens de nuvem do Ubuntu pode vir sem o netem. O pacote extra traz os módulos.
sudo apt-get update -qq || true
sudo apt-get install -y -qq "linux-modules-extra-$(uname -r)" >/dev/null 2>&1 \
    || echo "AVISO: não achei o pacote linux-modules-extra-$(uname -r). O TF-02 pode falhar se faltar o netem."
for modulo in sch_netem sch_htb cls_u32 ip_set ip_set_hash_ipport ip_set_hash_net xt_set; do
    if sudo modprobe "$modulo" 2>/dev/null; then echo "  módulo $modulo: ok"; else echo "  módulo $modulo: NÃO carregou"; fi
done
printf '%s\n' sch_netem sch_htb cls_u32 ip_set ip_set_hash_ipport ip_set_hash_net xt_set \
    | sudo tee /etc/modules-load.d/apurador-chaos.conf >/dev/null

echo "== 3. Imagens (o k3s usa o containerd, não o Docker) =="
if [ -f "$HOME/imagens.tar" ]; then
    sudo k3s ctr images import "$HOME/imagens.tar"
fi
IMAGENS=$(sudo k3s ctr images ls -q | grep -c 'apurador/' || true)
if [ "$IMAGENS" -lt 3 ]; then
    echo "ERRO: esperava 3 imagens apurador/* no containerd e achei $IMAGENS."
    sudo k3s ctr images ls -q | grep apurador || true
    exit 1
fi

echo "== 4. Sistema =="
IMAGENS_JA_NO_K3S=1 bash k8s/implantar.sh

echo "== 5. k6 =="
if ! command -v k6 >/dev/null 2>&1; then
    K6_VERSAO=${K6_VERSAO:-v1.3.0}
    ARQ=$(dpkg --print-architecture)
    curl -fsSL -o /tmp/k6.tgz "https://github.com/grafana/k6/releases/download/${K6_VERSAO}/k6-${K6_VERSAO}-linux-${ARQ}.tar.gz"
    tar xzf /tmp/k6.tgz -C /tmp
    sudo install "/tmp/k6-${K6_VERSAO}-linux-${ARQ}/k6" /usr/local/bin/k6
fi
k6 version

echo "== 6. Helm e Chaos Mesh =="
if ! command -v helm >/dev/null 2>&1; then
    curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
fi
bash chaos/instalar-chaos-mesh.sh

echo "== 7. Pronto =="
kubectl -n apurador get pods -o wide
kubectl -n chaos-mesh get pods
