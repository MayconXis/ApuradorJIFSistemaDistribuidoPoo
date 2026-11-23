# k3s no Mac, passo a passo

O k3s só roda em Linux. No Mac, o caminho é uma VM Ubuntu criada com o Multipass. O Docker Desktop fica só com o trabalho de gerar as imagens. Tudo o que o script faz está em `scripts/k3s-no-mac.sh` (no Mac) e em `scripts/vm/` (dentro da VM).

**SUPOSIÇÃO:** Mac com 16 GB de memória. A VM usa 4 CPUs e 8 GB (ajustável com `VM_CPUS` e `VM_MEMORIA`). Menos de 6 GB deixa o RabbitMQ, o PostgreSQL, o Dispatcher e os três workers apertados.

## 1. Antes de começar

1. Docker Desktop aberto e verde.
2. Multipass instalado: `brew install --cask multipass` ou o instalador de https://canonical.com/multipass/install. Os dois pedem a senha do Mac. Confira com `multipass version`.
3. Um terminal na pasta `apurador-jif` (a raiz do repositório).

## 2. Ensaio rápido sem k3s (opcional, recomendado)

```bash
docker compose up --build
```

Isso valida os mesmos Dockerfiles que o k3s vai usar e deixa a interface em http://localhost:4200. Para desligar: `docker compose down -v`. Faça isso antes do passo 3 para liberar memória.

## 3. Criar o cluster

```bash
bash scripts/k3s-no-mac.sh criar
```

O script gera as três imagens, cria a VM `apurador`, instala o k3s, importa as imagens, aplica os manifestos, instala o k6 e o Chaos Mesh. Na primeira vez leva de 10 a 20 minutos (a maior parte é o Maven dentro do `docker build`). No fim ele mostra o endereço da interface, algo como http://192.168.64.5/, que abre no navegador do Mac.

Conferir:

```bash
bash scripts/k3s-no-mac.sh pods
```

Esperado: `postgres-0`, `rabbitmq-0`, um `dispatcher`, `worker-0` a `worker-2` e um `frontend`, todos `Running` e prontos. Cada JVM leva cerca de 30 s para subir.

## 4. Ensaiar a demonstração

```bash
BASE=http://<ip-da-vm> bash scripts/demo/criar-chaves-demo.sh
multipass exec apurador -- sudo k3s kubectl -n apurador delete pod worker-1 --grace-period=0 --force
```

A segunda linha mata um worker enquanto o painel do cluster está aberto no navegador. Os estados esperados são VIVO, SUSPEITO (3 s), MORTO (5 s) e, depois que o pod volta, VIVO de novo.

## 5. Medir

Feche o Docker Desktop e os programas pesados, para o Mac ter memória livre. Depois:

```bash
bash scripts/k3s-no-mac.sh medir
```

Roda TC-01, TC-02, TF-01 (mata um worker com o Chaos Mesh), TF-02 (atrasa as mensagens), TF-02b (atrasa o heartbeat) e TF-03 (derruba o Gateway). Leva de 20 a 30 minutos. As saídas ficam em `carga/resultados/k3s/`. Cada arquivo é saída direta do k6, do observador do cluster ou do kubectl. Só estes arquivos podem entrar no relatório como "No k3s".

Limitações que precisam constar no relatório: o k6 roda dentro da própria VM e divide a CPU com o cluster, e a VM roda num notebook. O que o TF-02 e o TF-02b mostram só vale se os arquivos `tf02-prova-do-atraso.txt` e `tf02b-prova-do-atraso.txt` registrarem o atraso no tráfego (o `describe` do Chaos Mesh só diz que a regra foi instalada, não que o tráfego foi atrasado). Os atrasos miram o IP do Service e não o do pod, porque os workers falam com o RabbitMQ e com o Dispatcher pelo nome do Service.

Para repetir só algumas etapas, sem refazer as outras e sem tocar em `carga/resultados/k3s/`:

```bash
bash scripts/k3s-repetir.sh tf02 tf02b tf03
```

Etapas aceitas: `tc01 tc02 tf01 tf02 tf02b tf03`. As saídas vão para `carga/resultados/k3s-repeticao-<etapas>/` (se a pasta já existe, a nova leva o sufixo -2, -3 e a anterior fica intacta). Cada execução usa identificadores de evento próprios, para não reaproveitar os da anterior. Não precisa do Docker Desktop.

## 6. Depois

```bash
bash scripts/k3s-no-mac.sh atualizar   # depois de mudar código: gera as imagens de novo e reinicia
bash scripts/k3s-atualizar-frontend.sh # só a tela mudou: troca a imagem da interface sem reiniciar os workers
bash scripts/k3s-no-mac.sh parar       # desliga a VM
bash scripts/k3s-no-mac.sh apagar      # apaga a VM
```

## Se algo falhar

| Sintoma | O que olhar |
|---|---|
| `docker build` falha | A mensagem do Maven ou do npm. Os Dockerfiles foram escritos e validados na sintaxe, mas só rodam de verdade neste passo |
| `ctr images import` reclama de conteúdo ausente | Reconstruir sem o atestado extra: o script já usa `--provenance=false`; se o Docker for antigo, atualize o Docker Desktop |
| Pods em `ImagePullBackOff` | A imagem `apurador/*` não foi importada: `multipass exec apurador -- sudo k3s ctr images ls -q` |
| `tc` ou netem não funciona no TF-02 | `multipass exec apurador -- sudo modprobe sch_netem`. O passo 2 do `preparar-vm.sh` tenta instalar `linux-modules-extra` |
| Falta memória | `VM_MEMORIA=10G bash scripts/k3s-no-mac.sh criar` na primeira criação, ou feche outros programas |
