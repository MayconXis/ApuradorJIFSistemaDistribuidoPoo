# Matriz de rastreabilidade: Apurador JIF

Liga cada requisito ao componente que o atende, ao teste que o verifica e ao lugar em que ele aparece no relatório e na apresentação. A coluna Responsável das legendas segue a divisão do `plano-de-trabalho.md` (seção 1), ainda a confirmar com Felype e Maria Eduarda.

Correspondência dos identificadores com o documento de requisitos no modelo da disciplina: RF-01 é o RFn - 001, RNF-01 é o RnF - 001, e assim por diante.

## Legenda

**Componentes**

| Sigla | Componente | Onde fica | Responsável |
|---|---|---|---|
| CORE | Domínio de POO: modalidades, critérios, classificador | `core/` | Maria Eduarda |
| ANEL | Hash consistente com carga limitada | `cluster/` pacote `anel` | Maycon |
| DET | Detector de falhas por heartbeat | `cluster/` pacote `falhas` | Felype |
| COORD | Coordenador do Dispatcher: despacho, reatribuição, varredura | `cluster/` pacote `despacho` | Felype |
| APLIC | Aplicador idempotente de eventos | `cluster/` pacote `apuracao` | Felype |
| PERS | Entidades e repositórios JPA, esquema do banco | `persistencia/` | Felype |
| GW | Gateway e Dispatcher: API REST, publicação no RabbitMQ | `dispatcher/` | Maycon |
| WK | Worker: consumo da fila, heartbeat | `worker/` | Felype |
| FE | Interface Angular | `frontend/` | Maria Eduarda |
| K8S | Manifestos do k3s | `k8s/` | Maycon |

**Testes**

| ID | Teste | Tipo | Situação | Responsável |
|---|---|---|---|---|
| TU-01 | `FutsalEmpateTriploTest` | Unitário do core | Executado, passou (16 e 17/11/2026) | Maria Eduarda |
| TU-02 | `FutsalTest` | Unitário do core | Executado, passou (16 e 17/11/2026) | Maria Eduarda |
| TU-03 | `VoleibolTest` | Unitário do core | Executado, passou (16 e 17/11/2026) | Maria Eduarda |
| TU-04 | `HandebolTest` | Unitário do core | Executado, passou (16 e 17/11/2026) | Maria Eduarda |
| TU-05 | `ClassificadorTest` | Unitário do core | Executado, passou (16 e 17/11/2026) | Maria Eduarda |
| TU-06 | `ImutabilidadeTest` | Unitário do core | Executado, passou (16 e 17/11/2026) | Maria Eduarda |
| TU-07 | `NovaModalidadeTest` | Unitário do core | Executado, passou (16 e 17/11/2026) | Maria Eduarda |
| TA-01 | `AnelDeHashLimitadoTest` | Algoritmo de balanceamento | Executado, passou (16 e 17/11/2026) | Maycon |
| TA-02 | `DetectorDeFalhasTest` | Algoritmo de tolerância a falhas | Executado, passou (16 e 17/11/2026) | Felype |
| TA-03 | `CoordenadorTest` | Despacho e reatribuição | Executado, passou (16 e 17/11/2026) | Felype |
| TA-04 | `AplicadorDeEventosTest` | Idempotência e ordem | Executado, passou (16 e 17/11/2026) | Felype |
| TI-01 | `SimulacaoDeFalhaTest` | Integração simulada em memória | Executado, passou (16 e 17/11/2026) | Felype |
| TI-02 | `integracao/.../FluxoCompletoIT` | Integração com Dispatcher, dois workers, RabbitMQ e PostgreSQL | Executado, 7 de 7 (17/11/2026 e 23/11/2026) | Felype |
| TV-01 | `scripts/verificar-infra/verificar_infra.py` | Garantias do RabbitMQ e do PostgreSQL que o código assume | Executado, 4 de 4 (17/11/2026) | Maycon |
| TC-01 | `carga/rajada-resultados.js` | Carga com k6 | Executado fora do k3s (17/11/2026) e no k3s (18/11/2026, três rodadas, 0 perdidos) | Maycon |
| TC-02 | `carga/consultas-simultaneas.js` | Carga com k6 | Executado fora do k3s (17/11/2026) e no k3s (18/11/2026, p95 de 8,0 ms, 0% de falhas) | Maycon |
| TF-01 | `chaos/matar-worker.yaml` | Falha com Chaos Mesh | Equivalente com `kill -9` executado fora do k3s. No k3s (18/11/2026): executado com o Chaos Mesh, 0 perdidos, chaves com novo dono em 5750 ms | Felype |
| TF-02 | `chaos/atrasar-mensagens.yaml` | Falha com Chaos Mesh | No k3s (18/11/2026): atraso de 300 ms até o RabbitMQ, 0 perdidos, nenhum worker saiu de VIVO. A variante TF-02b (heartbeat de um worker atrasado em 2,5 s) deu 0 perdidos e um falso positivo do detector | Felype |
| TF-03 | Queda do Gateway com `kubectl delete pod` | Falha manual | Equivalente com `kill -9` executado fora do k3s. No k3s (18/11/2026): pod novo pronto em 10077 ms, 10,1% das requisições falharam, 0 eventos aceitos perdidos | Felype |
| TM-01 | Roteiro da demonstração | Manual | Pendente: sem registro de ensaio | Os três |
| TM-02 | Implantação do zero pelo README | Manual | Pendente: sem registro de execução | Maria Eduarda |

"Executado fora do k3s" quer dizer que o sistema inteiro rodou como processos locais, com RabbitMQ e PostgreSQL reais, e a saída está em `docs/03-relatorio/saida-dos-testes/medicoes-fora-do-k3s.txt`. "Executado" quer dizer que o teste rodou e a saída real está em `docs/03-relatorio/saida-dos-testes/`. Os testes TU, TA e TI-01 rodaram com javac e o JUnit Console Launcher, porque o ambiente de desenvolvimento não tinha acesso ao Maven Central. O TV-01 rodou em RabbitMQ 3.12.1 e PostgreSQL 16.15 e verifica a infraestrutura, não o código Java. "No k3s" quer dizer que a execução foi na VM Multipass do Maycon, em 18/11/2026, e as saídas estão em `carga/resultados/k3s/`, `k3s-verificacao/` e `k3s-repeticao-tf02-tf02b-tf03-2/` (cada pasta tem um `LEIA-ME.txt`). "Pendente" quer dizer que ainda ninguém executou.

**Relatório e apresentação**: R-n é a seção n do relatório técnico (`docs/03-relatorio/relatorio-tecnico.md`). L-n é a lâmina n do deck final de SD (`docs/04-apresentacao/apresentacao-sd.pptx`).

## Requisitos funcionais

| Requisito | Descrição curta | Componentes | Testes | Relatório | Lâmina |
|---|---|---|---|---|---|
| RF-01 | Cadastrar chave e gerar partidas | GW, PERS, FE | TI-02, TM-01 | R-3 | L-5 |
| RF-02 | Registrar resultado | FE, GW, PERS, COORD | TA-03, TI-01, TI-02 | R-3, R-6 | L-5, L-6 |
| RF-03 | Validar resultado pela modalidade | CORE, GW | TU-03, TI-02 | R-7 | L-3 |
| RF-04 | Registrar WO | CORE, GW | TU-02, TU-03, TU-04 | R-7 | L-3 |
| RF-05 | Calcular pontuação e ordenar | CORE, APLIC | TU-01 a TU-05 | R-7 | L-3 |
| RF-06 | Desempatar pela ordem do regulamento | CORE | TU-01, TU-02, TU-03, TU-04, TU-05 | R-7 | L-1, L-3 |
| RF-07 | Informar o critério decisivo | CORE, FE | TU-01, TU-05, TM-01 | R-7 | L-5 |
| RF-08 | Sinalizar sorteio pendente | CORE, FE | TU-05 | R-9 | L-3 |
| RF-09 | Consultar classificação | GW, PERS, FE | TI-02, TC-02 | R-3 | L-5 |
| RF-10 | Retificar e recalcular | GW, APLIC, PERS | TA-04, TI-01 | R-6, R-9 | L-4 |
| RF-11 | Consultar andamento do evento | GW, PERS | TI-02 | R-3 | L-5 |
| RF-12 | Painel do cluster | FE, GW, COORD | TA-03, TM-01 | R-3 | L-6, L-9 |
| RF-13 | Listar chaves e partidas | GW, PERS, FE | TI-02 | R-3 | L-5 |
| RF-14 | Isolar mensagem que falha (DLQ) | WK, GW | TV-01, TI-02, TF-02 | R-6 | L-7 |
| RF-15 | Auditar a chave contra o histórico | GW, CORE, PERS | TI-01, TC-01, TF-01 | R-8 | L-10 |

## Requisitos não funcionais

| Requisito | Descrição curta | Componentes | Testes | Relatório | Lâmina |
|---|---|---|---|---|---|
| RNF-01 | Disponibilidade na queda de worker | DET, COORD, ANEL, K8S | TA-03, TI-01, TI-02, TF-01 | R-6, R-8 | L-9, L-10 |
| RNF-02 | Durabilidade, zero perdidos | GW, PERS, COORD | TV-01, TI-01, TI-02, TC-01, TF-01 | R-6, R-8 | L-9, L-10 |
| RNF-03 | Idempotência, zero duplicatas | APLIC, PERS | TV-01, TA-04, TI-01, TI-02, TF-01 | R-6, R-8 | L-9, L-10 |
| RNF-04 | Ordenação por chave | ANEL, APLIC, PERS | TV-01, TA-04, TI-01 | R-5 | L-8 |
| RNF-05 | Hash consistente com carga limitada | ANEL, COORD | TA-01, TC-01 | R-5 | L-8 |
| RNF-06 | Detector por heartbeat | DET, COORD, WK | TA-02, TA-03, TF-01 | R-6 | L-9 |
| RNF-07 | Movimento mínimo de chaves | ANEL | TA-01 | R-5 | L-8 |
| RNF-08 | Observabilidade | GW, WK, FE | TM-01 | R-3 | L-6 |
| RNF-09 | Implantação reproduzível | K8S, README | TM-02 | R-3 | L-7, L-11 |
| RNF-10 | Nova modalidade sem mudar o Classificador | CORE | TU-07 | R-7 | L-3 |
| RNF-11 | Core testável sem infraestrutura | CORE | TU-01 a TU-07 | R-7 | L-10 |
| RNF-12 | Latência sob rajada | GW, WK, PERS | TC-01, TC-02 | R-8 | L-10 |
| RNF-13 | Restrições de tecnologia | Todos | Revisão de código | R-4 | L-7 |
| RNF-14 | Recuperação do Gateway | COORD, PERS, K8S | TA-03, TF-03 | R-6, R-9 | L-6, L-11 |

## Regras de negócio

| Regra | Artigo | Classe do core | Teste |
|---|---|---|---|
| RN-01, RN-02 | Arts. 28 e 29 | `Chave` | TU-05 |
| RN-03 | Art. 61 | `ModalidadeDeGols`, `Futsal` | TU-02 |
| RN-04 | Art. 61, parágrafo único | `ModalidadeDeGols.placarDeWO` | TU-02 |
| RN-05 | Art. 62, § 1º | `Futsal.criterios` | TU-02 |
| RN-06 | Art. 62, § 2º | `ConfrontoDireto.aplicavelA` | TU-01 |
| RN-07 | Art. 66 | `ModalidadeDeGols`, `Handebol` | TU-04 |
| RN-08 | Art. 67, § 1º | `Handebol.criterios` | TU-04 |
| RN-09 | Art. 67, § 2º | `Handebol.criterios` | TU-04 |
| RN-10 | Art. 67 (tabela de sanções) | `Disciplina`, `MenosSancoes` | TU-04 |
| RN-11 | Art. 111 | `Voleibol.validar` | TU-03 |
| RN-12 | Art. 112 | `Voleibol.pontosPara` | TU-03 |
| RN-13 | Art. 112, parágrafo único | `Voleibol.placarDeWO` | TU-03 |
| RN-14 | Art. 113 | `Voleibol.criterios` | TU-03 |
| RN-15 | Art. 135, parágrafo único | Motiva RF-07 e RF-10 | TA-04 |

## Suposições

| Suposição | Onde está isolada no código | Teste |
|---|---|---|
| SUP-01 | `Classificador.recomecaQuandoOEmpateDiminui` | TU-01, TU-05 |
| SUP-02 | `ModalidadeDeGols.placarDeWO` | TU-02 |
| SUP-03 | `CriterioDeAverage.comparar` | TU-03, TU-04 |
| SUP-04 | `Estatisticas` (recebe os jogos da chave) | TU-02 |
| SUP-05 | `Handebol.criterios` | TU-04 |
| SUP-06 | `Sorteio` e `Posicao.isPendenteDeSorteio` | TU-05 |
| SUP-07 | `EstadoDaPartida` (persistência) | TA-04 |
| SUP-08 | `AplicadorDeEventos` (retificação) | TA-04 |
