# Apurador JIF

Sistema distribuído que recebe os resultados das partidas da fase classificatória dos Jogos das Instituições Federais (JIF), calcula a classificação de cada chave pela regra da modalidade e mostra o critério que decidiu cada posição. Trabalho final de Programação Orientada a Objetos e de Sistemas Distribuídos.

Equipe: Maycon Douglas da Silva, Felype de Moura Lino e Silva, Maria Eduarda Mendonça Fernandes. IFG Câmpus Inhumas, Engenharia de Software.

## O que foi executado e o que não foi

Esta seção existe para ninguém confundir meta com resultado.

| Parte | Situação | Onde está a saída |
|---|---|---|
| Testes do `core` (TU-01 a TU-07, 37 testes) | Executados, todos passando | `docs/03-relatorio/saida-dos-testes/core.txt` |
| Testes do `cluster` (TA-01 a TA-04 e TI-01, 41 testes) | Executados, todos passando | `docs/03-relatorio/saida-dos-testes/cluster.txt` |
| Esquema do banco em um PostgreSQL 16 de verdade | Executado | `docs/03-relatorio/saida-dos-testes/infra.txt` |
| Garantias do RabbitMQ e do PostgreSQL que o código assume (reentrega sem ack, limite de entregas e DLQ, trava da chave, evento repetido) | Executado em RabbitMQ 3.12 e PostgreSQL 16 | `docs/03-relatorio/saida-dos-testes/infra.txt` |
| Manifestos do k3s | Validados contra o esquema do Kubernetes 1.33 (kubeconform) e aplicados num k3s v1.37.2, numa VM Multipass no Mac, em 18/11/2026 | `carga/resultados/k3s/ambiente.txt` |
| Manifestos do Chaos Mesh | Conferidos campo a campo contra os CRDs do Chaos Mesh 2.7.2 e aplicados no k3s com o Chaos Mesh 2.8.8 | `carga/resultados/k3s/` |
| Scripts do k6 | Executados contra o sistema fora do k3s e no k3s | `carga/resultados/` |
| Interface Angular | Compilada e usada contra o sistema real | Capturas em `docs/04-apresentacao/telas/` |
| Build completo com Maven (7 módulos) | Executado no Mac do grupo em 17/11/2026, sem erro. Depois da reorganização do código em camadas (23/11/2026), compilado e testado em um Linux com JDK 21: 78 testes e TI-02 passando. **A repetir no Mac:** `bash scripts/compilar-no-mac.sh` | `docs/03-relatorio/saida-dos-testes/build-maven-mac.txt` (versão anterior à reorganização) e `ti-02.txt` |
| TI-02: Dispatcher e dois workers com RabbitMQ e PostgreSQL reais | Executado: 7 de 7, em 17/11/2026 e de novo em 23/11/2026, depois da reorganização. Como rodar: seção 1 | `docs/03-relatorio/saida-dos-testes/ti-02.txt` |
| TC-01, TC-02 e quedas com `kill -9` (worker e Gateway) | Executados **fora do k3s**, com o sistema inteiro em processos locais | `docs/03-relatorio/saida-dos-testes/medicoes-fora-do-k3s.txt` e `carga/resultados/` |
| TC-01, TC-02, TF-01, TF-02, TF-02b e TF-03 no k3s | Executados em 18/11/2026 numa VM Multipass de 4 CPUs e 8 GB num MacBook Air, com o k6 dentro da VM. 0 eventos aceitos perdidos nas rodadas válidas. Números no relatório, seções 6.4 e 8.3 | `carga/resultados/k3s/`, `k3s-verificacao/` e `k3s-repeticao-tf02-tf02b-tf03-2/`, cada uma com um `LEIA-ME.txt` que diz o que vale |
| TM-01, TM-02 | **Pendente** | O grupo ensaia a demonstração e executa a implantação do zero pelo README |

Os números de carga e de falha (k6 e k3s) são de 17 e 18/11/2026, anteriores à reorganização do código em camadas. A reorganização mudou a estrutura dos pacotes, não a lógica.

Para compilar tudo em um Mac com acesso à internet: `bash scripts/compilar-no-mac.sh`. O script baixa um JDK 21 e o Maven para dentro da pasta, se faltarem, e grava a saída em `../_fora-da-entrega/_logs/`.

## Módulos

| Módulo | O que é | Depende de Spring? |
|---|---|---|
| `core` | Domínio de POO: `Modalidade`, `CriterioDesempate`, `Classificador`, `Chave`, `Partida`, `Placar`, `Posicao` | Não |
| `cluster` | Os dois algoritmos do grupo: anel de hash consistente com carga limitada e detector de falhas por heartbeat. Também o coordenador do Dispatcher e o aplicador idempotente de eventos | Não |
| `persistencia` | Entidades JPA, repositórios Spring Data, mapeadores e os adaptadores que ligam o banco às interfaces do `cluster` (`HistoricoJpa`, `RegistroJpa`). Esquema do PostgreSQL em `db/schema.sql` | Sim |
| `dispatcher` | Gateway e Dispatcher: API REST em camadas (controller, service, validator, mapper), anel em memória, publicação no RabbitMQ, varredura de pendentes | Sim |
| `worker` | Consome a própria fila (`listener`), aplica os eventos pendentes da chave e envia heartbeat (`scheduler`) | Sim |
| `integracao` | TI-02: Dispatcher e dois workers com RabbitMQ e PostgreSQL de verdade. Só entra com `-Pintegracao` | Sim |
| `frontend` | Interface Angular: registrar resultado, classificação, painel do cluster | Não (Angular) |

Pastas de apoio: `docker/` (imagens e nginx), `k8s/` (manifestos do k3s), `carga/` (k6), `chaos/` (Chaos Mesh e scripts de falha), `scripts/`, `docs/`.

O Lombok (1.18.48) entra só em `persistencia`, `dispatcher` e `worker`, como processador de anotações. `core` e `cluster` não dependem de Spring nem de Lombok. O Hibernate apenas confere as entidades contra o esquema (`ddl-auto: validate`): quem cria as tabelas é o `db/schema.sql`.

## Organização do código

**Back-end.** Os módulos Spring seguem a mesma divisão de camadas.

| Pacote | Módulo | Papel |
|---|---|---|
| `controller` | `dispatcher` | 4 controllers (`ChaveController`, `ResultadoController`, `ClusterController`, `ModalidadeController`): recebem o HTTP, validam com `@Valid`, devolvem o status e delegam ao serviço |
| `service` | `dispatcher` | 4 serviços coesos por domínio (`ChaveService`, `ResultadoService`, `ClusterService`, `ModalidadeService`). Abrem a transação quando precisam |
| `validator` | `dispatcher` | Regras de validação complementares ao Bean Validation (`ChaveValidator`, `ResultadoValidator`), lançando `ResponseStatusException` |
| `mapper` | `dispatcher`, `persistencia` | Converte entre entidade, objeto do `core` e DTO, com métodos estáticos |
| `dto/request`, `dto/response` | `dispatcher` | O formato do JSON de entrada (com anotações Bean Validation) e de saída |
| `handler` | `dispatcher` | `GlobalExceptionHandler`: transforma exceções de validação e negócio em `ErroResponse` (`status`, `erro`, `timestamp`, `camposInvalidos`) |
| `config`, `scheduler`, `messaging` | `dispatcher`, `worker` | Beans do anel e do RabbitMQ, tarefas periódicas e a publicação das tarefas |
| `domain`, `repository`, `adapter` | `persistencia` | Entidades JPA, repositórios Spring Data e os adaptadores `HistoricoJpa` e `RegistroJpa` |
| `listener` | `worker` | `TarefaListener`, o consumidor da fila do worker, com ack manual |

**Interface Angular** (`frontend/src/app`).

| Pasta | O que tem |
|---|---|
| `config` | URL da API, intervalos de atualização e o título da aba |
| `models` | Os formatos do Gateway, com os mesmos nomes dos DTOs do back (`ChaveResponse`, `ClassificacaoResponse`, `ResultadoRegistrarRequest`...) |
| `services` | Um serviço por recurso (`ChavesService`, `ResultadosService`, `ClusterService`, `ModalidadesService`), sobre o `HttpClient` |
| `utils`, `pipes` | Texto de erro da API, identificador do envio e formatação de saldo, porcentagem e segundos |
| `components` | `layout` (topo, menu e rodapé, com as telas dentro) e `alerta` |
| `pages` | `registrar`, `classificacao` e `cluster`, cada uma com os componentes que só ela usa |

## 1. Testes sem infraestrutura

Precisa de JDK 21 e Maven 3.9.

```bash
mvn -pl core,cluster test
```

Sem Maven, com o JUnit Console Launcher (é como os testes foram executados até agora):

```bash
bash scripts/testar-sem-maven.sh
```

### TI-02, com RabbitMQ e PostgreSQL

O teste de integração sobe o Dispatcher e dois workers na mesma JVM, contra um RabbitMQ e um PostgreSQL de verdade. Com Docker, o Testcontainers cria o PostgreSQL 16 e o RabbitMQ 3.13 sozinho:

```bash
mvn -Pintegracao -pl integracao -am verify
```

Sem Docker, aponte para um PostgreSQL e um RabbitMQ que já estejam rodando:

```bash
APURADOR_IT_EXTERNO=1 mvn -Pintegracao -pl integracao -am verify
```

Nesse modo, as variáveis abaixo trocam o endereço. Os valores da tabela são os padrões.

| Variável | Padrão |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/apurador` |
| `DB_USER`, `DB_PASSWORD` | `apurador`, `apurador` |
| `RABBIT_HOST`, `RABBIT_PORT` | `localhost`, `5672` |
| `RABBIT_USER`, `RABBIT_PASSWORD` | `guest`, `guest` |

O teste cria o esquema com `db/schema.sql` e **apaga os dados** das tabelas do banco indicado. Use um banco só para isso.

## 2. Rodar tudo em uma máquina, sem k3s

Precisa de Docker com o Compose.

```bash
docker compose up --build
```

| Endereço | O que é |
|---|---|
| http://localhost:4200 | Interface |
| http://localhost:8080/api/cluster | API do Gateway (exemplo) |
| http://localhost:15672 | Painel do RabbitMQ (guest / guest) |

Na tela "Registrar resultado", o botão "Carregar exemplo do empate triplo" cria a chave IFCE, IFG e IFSP com os três placares do teste TU-01.

## 3. Implantação no k3s, do zero

**SUPOSIÇÃO:** uma máquina Linux (ou uma VM Linux) com 4 núcleos e 8 GB de memória livres. O k3s não roda direto no macOS nem no Windows.

```bash
# 1. k3s (já vem com o Traefik, que faz o papel de Ingress)
curl -sfL https://get.k3s.io | sh -
mkdir -p ~/.kube && sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config && sudo chown $USER ~/.kube/config

# 2. Docker, para gerar as imagens (o k3s usa o containerd, as imagens são importadas pelo script)
curl -fsSL https://get.docker.com | sh

# 3. Imagens, importação no k3s e manifestos
bash k8s/implantar.sh
```

O `implantar.sh` gera as três imagens, importa no containerd do k3s, cria o ConfigMap do esquema a partir de `persistencia/src/main/resources/db/schema.sql` e aplica os manifestos na ordem: banco e RabbitMQ, Dispatcher, workers, interface e Ingress. No fim ele mostra o endereço da interface.

Para conferir:

```bash
kubectl -n apurador get pods
curl http://<ip-do-nó>/api/cluster
```

Para apagar tudo, inclusive os dados: `bash k8s/remover.sh`.

**No macOS.** O k3s só roda em Linux. Foi assim que o grupo montou e mediu o cluster: `bash scripts/k3s-no-mac.sh criar` cria uma VM Ubuntu com o Multipass, instala o k3s e o Chaos Mesh e aplica os manifestos. O passo a passo está em `scripts/k3s-no-mac.md`. As medições do relatório saíram de `bash scripts/k3s-no-mac.sh medir`, e `bash scripts/k3s-repetir.sh tf02 tf02b tf03` repete só algumas etapas.

### O que cada manifesto faz

| Arquivo | Recurso | Observação |
|---|---|---|
| `00-namespace.yaml` | Namespace `apurador` | |
| `01-configuracao.yaml` | ConfigMap e Secret | Parâmetros dos algoritmos (SUPOSIÇÃO, a calibrar) |
| `02-postgres.yaml` | StatefulSet com 1 réplica | O esquema entra pelo `/docker-entrypoint-initdb.d` |
| `03-rabbitmq.yaml` | StatefulSet com 1 nó | Fila quórum com um nó não tem réplica. Ver limitações |
| `04-dispatcher.yaml` | Deployment com 1 réplica, `Recreate` | O anel fica em memória: nunca podem existir dois |
| `05-worker.yaml` | StatefulSet com 3 réplicas | O nome do pod (`worker-0`) é o id no anel e o nome da fila |
| `06-frontend.yaml` | Deployment do nginx com o Angular | O nginx repassa `/api` para o Dispatcher |
| `07-ingress.yaml` | Ingress do Traefik | Interface em `/` |

As probes de liveness e readiness usam o Actuator. Elas complementam o detector de falhas do grupo, não o substituem: a probe reinicia o pod, e o detector é quem tira o worker do anel.

## 4. Testes de carga (k6)

Os limites (thresholds) dos scripts são provisórios. A primeira execução serve de linha de base.

```bash
# TC-01: rajada de resultados, com 10% de reenvios do mesmo evento
k6 run -e BASE=http://<ip-do-nó> -e CHAVES=30 -e TAXA=40 -e DURACAO=60s carga/rajada-resultados.js

# TC-02: consultas simultâneas à classificação (rodar depois do TC-01)
k6 run -e BASE=http://<ip-do-nó> -e USUARIOS=200 carga/consultas-simultaneas.js
```

No fim do TC-01 o script espera a fila esvaziar e confere evento por evento. A saída vai para `carga/resultados/`. As métricas que respondem à meta do trabalho são:

| Métrica | De onde vem |
|---|---|
| Eventos perdidos | Eventos novos aceitos com 202 menos eventos encontrados no banco |
| Duplicatas | `aplicadosMaisDeUmaVez` e `chavesInconsistentes` do `GET /api/consistencia` |
| Latência do registro | `http_req_duration{tipo:registro}` |
| Tempo até esvaziar | Do fim da rajada até não haver evento pendente |

**HIPÓTESE:** os resultados chegam em rajada no fim das rodadas e muita gente consulta ao mesmo tempo. Não há dado do JIF que sustente isso. Os testes reproduzem a hipótese, não a comprovam.

## 5. Testes de falha (Chaos Mesh)

```bash
bash chaos/instalar-chaos-mesh.sh
```

| Teste | Como rodar | O que medir |
|---|---|---|
| TF-01 matar um worker | Com o TC-01 rodando: `kubectl apply -f chaos/matar-worker.yaml` | Perdidos, duplicatas e tempo de recuperação |
| TF-02 atrasar mensagens, TF-02b atrasar o heartbeat | O arquivo `chaos/atrasar-mensagens.yaml` é um modelo com os IPs dos Services (o atraso precisa mirar o IP do Service, não o do pod). `scripts/vm/medir-no-k3s.sh` preenche os IPs, aplica cada variante com o TC-01 rodando e grava a prova do atraso. À mão, o comentário no início do arquivo mostra o comando | Perdidos, duplicatas, falsos positivos do detector e a prova de que o atraso chegou ao tráfego |
| TF-03 derrubar o Gateway | Com o TC-01 rodando: `BASE=http://<ip-do-nó> bash chaos/derrubar-gateway.sh` | Tempo até voltar. Nenhum evento aceito pode se perder |

Para medir o tempo de recuperação, deixe o observador rodando em outro terminal:

```bash
python3 chaos/observar_cluster.py --base http://<ip-do-nó> --intervalo 0.2
```

Ele registra o instante em que o worker sai de VIVO, quando é declarado MORTO e quando as chaves dele têm novo dono.

**Comportamento esperado no k3s:** o StatefulSet recria o pod morto com o mesmo nome. Se o Spring Boot demorar mais que o tempo limite do heartbeat (5 s) para subir, o detector declara o worker morto, as chaves mudam de dono e, quando o pod volta, ele recebe chaves de novo. Se o pod voltar antes do tempo limite, o detector não chega a declarar a morte, e a mensagem não confirmada volta para a fila do mesmo worker. Nos dois casos nada se perde nem se duplica. No k3s, no TF-01, o pod voltou cerca de 10,4 s depois do PodChaos, depois de o worker ser declarado MORTO (em 5750 ms), então o caminho foi o da reatribuição (relatório, seção 6.4).

## 6. Parâmetros a calibrar

Todos começam como SUPOSIÇÃO. Ficam em `k8s/01-configuracao.yaml` (k3s) ou nos `application.yaml` do `dispatcher` e do `worker`.

| Parâmetro | Variável | Valor inicial |
|---|---|---|
| Pontos virtuais por worker | `ANEL_PONTOS` | 64 |
| Folga do teto de carga | `ANEL_FOLGA` | 0.25 |
| Intervalo do heartbeat | `HEARTBEAT_INTERVALO_MS` | 1000 |
| Limite para suspeita | `HEARTBEAT_SUSPEITA_MS` | 3000 |
| Tempo limite do heartbeat | `HEARTBEAT_LIMITE_MS` | 5000 |
| Limite de devoluções antes da DLQ | `FILA_LIMITE_ENTREGAS` | 5 |
| Atraso artificial por tarefa (só demonstração) | `WORKER_ATRASO_MS` | 0 |

Medido no RabbitMQ 3.12: com limite N, a mensagem é entregue N + 1 vezes antes de ir para a DLQ (`infra.txt`, verificação V2).

## 7. Decisões de implementação que valem explicar

**Por que polling e não SSE na interface.** A tela consulta o Gateway a cada 2 segundos e compara o número de `versao` retornado para decidir se precisa recarregar a lista de partidas. Com SSE, cada tela aberta seguraria uma conexão no Dispatcher, que é uma réplica só, e o nginx e o Traefik precisariam de configuração para não guardar o fluxo em buffer. Um atraso de até 2 segundos não pesa diante do prazo de 2 horas para protesto (Art. 135, parágrafo único). Polling simples foi o suficiente para resolver o problema sem complexidade extra.

**Por que o evento vai para o banco antes da fila.** O Gateway grava o resultado como PENDENTE no PostgreSQL e só depois publica a tarefa. A mensagem diz apenas "esta chave tem eventos pendentes". Se a publicação falhar, a varredura do Dispatcher reenvia. Se o worker cair antes de confirmar, o RabbitMQ entrega de novo e o novo dono não encontra nada pendente que já tenha sido aplicado. É isso que sustenta a meta de zero perdidos e zero duplicatas.

**Por que um consumidor por fila.** Cada fila é a partição de um worker, e os eventos de uma chave vão sempre para a fila do dono dela. Com um consumidor e prefetch 1, a ordem dentro da partição se mantém. A trava da linha da chave no banco protege o caso em que dois workers tocam a mesma chave durante uma reatribuição.

## 8. Limitações assumidas

- O Gateway/Dispatcher é ponto único de falha. Roda em uma réplica, com probes e reinício automático, e reconstrói o anel a partir dos heartbeats e das chaves gravadas. Eleição de líder fica como evolução.
- PostgreSQL e RabbitMQ rodam com uma instância cada. A fila quórum só tem réplica com três nós do RabbitMQ.
- Não há login nem autorização. Qualquer pessoa com acesso à interface registra resultado.
- O sorteio, último critério de todas as modalidades, acontece fora do sistema. A tabela só marca a posição como "Sorteio pendente".

## Documentação

| Documento | Onde |
|---|---|
| Requisitos no modelo da disciplina de APS | `docs/00-requisitos/documento-de-requisitos.odt` |
| Especificação complementar (regras com artigo, Gherkin) | `docs/00-requisitos/especificacao-complementar.md` |
| Matriz de rastreabilidade | `docs/00-requisitos/matriz-rastreabilidade.md` |
| Plano de trabalho e de testes | `docs/00-requisitos/plano-de-trabalho.md` |
| Diagramas (draw.io) | `docs/diagramas/` |
| Proposta de solução | `docs/01-proposta/proposta-de-solucao.md` |
| Saídas reais dos testes | `docs/03-relatorio/saida-dos-testes/` |
