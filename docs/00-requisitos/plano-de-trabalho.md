# Plano de trabalho e plano de testes: Apurador JIF

## 1. Divisão do trabalho

**Situação.** Divisão definida a pedido do Maycon, que deixou a escolha em aberto. Felype e Maria Eduarda ainda não confirmaram. O que já vale desde a Ata 003 é a fala do Maycon na apresentação: capa, infraestrutura e balanceamento. Se alguém quiser trocar uma parte, o critério é este: cada pessoa tem de conseguir explicar sozinha, na arguição, tudo o que está na sua coluna, e o revisor tem de conseguir explicar a parte do colega.

### 1.1 Visão geral

| Pessoa | Frente | O que responde na arguição | Revisor | Lâminas (tempo) |
|---|---|---|---|---|
| Maycon Douglas da Silva | Infraestrutura e balanceamento | Por que k3s, RabbitMQ e uma fila por worker. Como o hash consistente com carga limitada distribui as chaves | Felype | 1, 7 e 8 (3 min 35 s) |
| Felype de Moura Lino e Silva | Tolerância a falhas e integração | Como um worker morto é detectado, as chaves dele são reatribuídas e os eventos reprocessados sem perder nem duplicar | Maria Eduarda | 6, 9, 10 e 11 (4 min 30 s) |
| Maria Eduarda Mendonça Fernandes | Domínio de POO, requisitos e interface | Como a classificação e o desempate são calculados, por que o empate triplo dá aquele resultado, como a interface usa a API | Maycon | 2, 3, 4 e 5 (4 min 20 s) |

### 1.2 Maycon: infraestrutura e balanceamento

| Item | O que fica com ele |
|---|---|
| Código | `cluster/` pacote `anel` (hash consistente com carga limitada), `dispatcher/` (Gateway e Dispatcher), `k8s/`, `docker/`, `docker-compose.yml`, `scripts/` (k3s no Mac) e `carga/` (k6) |
| Testes | TA-01 (anel), TV-01 (garantias do RabbitMQ e do PostgreSQL), TC-01 e TC-02 (k6). TM-02: ele corrige o que o README errar |
| Relatório | 3.2, 4 (linhas de middleware, VIM, filas e Dispatcher), 5, 8.3 e, na 9, os itens de infraestrutura |
| Demonstração | Digita os comandos no terminal (passos 4 e 6 do roteiro) |
| Medição no k3s | Sobe a máquina virtual, roda `scripts/k3s-no-mac.sh medir` e entrega ao grupo a pasta `carga/resultados/k3s/` |
| Estudar para a arguição | Seções 3 e 5 de `docs/04-apresentacao/perguntas-da-arguicao.md` |

### 1.3 Felype: tolerância a falhas e integração

| Item | O que fica com ele |
|---|---|
| Código | `cluster/` pacotes `falhas` (detector por heartbeat), `despacho` (reatribuição e varredura) e `apuracao` (aplicador idempotente e trava por chave), `worker/`, `persistencia/` e `chaos/` |
| Testes | TA-02, TA-03, TA-04, TI-01, TI-02, TF-01, TF-02 e TF-03 |
| Relatório | 6 (com a 6.5), 8.4 e, na 4, as linhas de ordem de gravação, conteúdo da mensagem, exclusão mútua por chave e recálculo da tabela. Na 9, o detector por tempo limite e o Gateway como ponto único de falha |
| Demonstração | Narra o painel Cluster (passos 1, 5, 7 e 9 do roteiro) |
| Medição no k3s | Aplica os experimentos do Chaos Mesh e confere os falsos positivos do detector |
| Estudar para a arguição | Seção 4 de `docs/04-apresentacao/perguntas-da-arguicao.md` |

### 1.4 Maria Eduarda: domínio de POO, requisitos e interface

| Item | O que fica com ela |
|---|---|
| Código | `core/` (modalidades, critérios, classificador e chave) e `frontend/` (Angular) |
| Documentos | Fase 0 inteira (documento de requisitos, especificação complementar, matriz de rastreabilidade, atas, diagramas e protótipos) e a proposta da Fase 1 |
| Testes | TU-01 a TU-07. TM-02: executa a implantação do zero seguindo o README, porque quem não montou a infraestrutura enxerga o que falta no texto |
| Relatório | 1, 2, 3.1, 7, 8.1, 8.2, 10, 11 e, na 9, as leituras do regulamento, a ausência de autenticação e o sorteio fora do sistema |
| Demonstração | Opera a interface (passos 2, 3, 8, 10 e 11 do roteiro) |
| Depois da medição | Atualiza a matriz de rastreabilidade com os números medidos e prepara o rascunho da Ata 004 |
| Estudar para a arguição | Seções 2 e 6 de `docs/04-apresentacao/perguntas-da-arguicao.md` |

### 1.5 O que é dos três

- **Visão geral e testes na arguição.** Todos estudam as seções 1 e 7 de `perguntas-da-arguicao.md`. Todos sabem explicar por que o evento é gravado no banco antes de ir para a fila e o que cada medição prova e não prova.
- **Revisão cruzada.** Depois que os números medidos entram no relatório, cada revisor lê a seção do colega e anota o que não entendeu. O que o revisor não entende, a arguição vai perguntar.
- **Ensaio.** A demonstração (TM-01) é ensaiada duas vezes seguidas, cada um no seu papel, antes da data da apresentação.
- **Atas.** O grupo preenche data, hora, local, presença e responsáveis. Nada disso foi preenchido pelo assistente.
- **Equilíbrio.** O Maycon tem mais infraestrutura e medição, o Felype tem mais código de algoritmo e de integração, a Maria Eduarda tem mais documentação e interface. Se o grupo achar a carga desigual, as trocas mais baratas são passar o TV-01 do Maycon para a Maria Eduarda ou a seção 8.4 do Felype para o Maycon.

## 2. Cronograma

**SUPOSIÇÃO.** Não sabemos o prazo de entrega. O cronograma abaixo assume seis semanas e precisa ser ajustado à data real.

| Semana | Entrega | Responsável | Critério de pronto |
|---|---|---|---|
| 1 | Requisitos, atas, diagramas e proposta revisados pelo grupo | Maria Eduarda, com revisão dos três | Atas assinadas, proposta enviada ao professor |
| 2 | Core rodando com os testes | Maria Eduarda | `mvn -pl core test` sem falhas |
| 3 | Algoritmos de balanceamento e de tolerância a falhas com os testes | Maycon (anel) e Felype (falhas) | `mvn -pl cluster test` sem falhas |
| 4 | Dispatcher, worker e interface rodando em `docker compose` | Maycon (Dispatcher e compose), Felype (worker), Maria Eduarda (interface) | Registrar um resultado e ver a tabela mudar |
| 5 | Implantação no k3s, k6 e Chaos Mesh | Maycon (k3s e k6), Felype (Chaos Mesh) | Resultados medidos e Ata 04 preenchida |
| 6 | Relatório fechado, ensaio da demonstração e da arguição | Os três | Demonstração feita duas vezes sem erro |

## 3. Plano de testes

### 3.1 Objetivo

Mostrar que o sistema calcula a classificação certa (POO) e que continua certo sob carga e sob falha (SD). A meta de consistência é zero eventos perdidos e zero duplicatas. Meta não é resultado: os números só entram no relatório depois de medidos.

### 3.2 Níveis de teste

| Nível | IDs | Ferramenta | Precisa de infraestrutura? |
|---|---|---|---|
| Unitário do core | TU-01 a TU-07 | JUnit 5 | Não |
| Algoritmos | TA-01 a TA-04 | JUnit 5, relógio manual | Não |
| Integração simulada | TI-01 | JUnit 5, fila e histórico em memória | Não |
| Integração real | TI-02 | Spring Boot Test, Testcontainers | Docker, ou RabbitMQ e PostgreSQL já rodando |
| Verificação da infraestrutura | TV-01 | Python, pika, psycopg2 | RabbitMQ e PostgreSQL |
| Carga | TC-01, TC-02 | k6 | k3s |
| Injeção de falhas | TF-01, TF-02, TF-03 | Chaos Mesh, kubectl | k3s |
| Manual | TM-01, TM-02 | Roteiro e README | k3s |

### 3.3 Casos principais

| ID | Cenário | Critério para passar |
|---|---|---|
| TU-01 | Empate triplo no futsal (IFCE 4x1 IFG, IFG 3x1 IFSP, IFSP 4x3 IFCE) | IFCE em 1º por saldo, IFG em 2º por confronto direto, IFSP em 3º |
| TA-01 | 3 workers, muitas chaves, entrada e saída de workers | Nenhum worker acima do teto. Na saída de um worker, só as chaves dele mudam |
| TA-02 | Heartbeats que param | Worker passa a suspeito e depois a morto nos tempos configurados. Heartbeat novo o traz de volta |
| TA-03 | Worker declarado morto | Chaves reatribuídas e eventos pendentes reenviados ao novo dono |
| TA-04 | Mensagem repetida, mensagem fora de ordem, retificação | Tabela igual à do core, cada evento aplicado uma vez |
| TI-01 | Rajada com morte de um worker no meio | Zero perdidos, zero duplicatas, tabela igual à calculada direto pelo core |
| TC-01 | Rajada de resultados com eventos repetidos de propósito | Erros HTTP e latência dentro do limite. Auditoria sem divergência |
| TC-02 | Muitos usuários virtuais consultando a tabela | Latência dentro do limite |
| TF-01 | Chaos Mesh mata um worker durante TC-01 | Zero perdidos, zero duplicatas. Tempo de recuperação medido |
| TF-02 | Chaos Mesh atrasa a rede entre workers e RabbitMQ. Variante TF-02b: atrasa o heartbeat de um worker até o Dispatcher | Sem perda nem duplicata. Falsos positivos do detector contados |
| TF-03 | Gateway derrubado durante a rajada | Nenhum evento aceito é perdido. Tempo até voltar medido |

### 3.4 Métricas

A coluna Resultado vem das medições no k3s de 18/11/2026 (VM Multipass de 4 CPUs e 8 GB num MacBook Air). A origem de cada número está no relatório técnico, seção 8.3.

| Métrica | Como é obtida | Meta | Resultado |
|---|---|---|---|
| Eventos perdidos | Eventos aceitos pelo Gateway menos eventos aplicados, depois de a fila esvaziar | 0 | 0 em todas as rodadas válidas |
| Duplicatas na tabela final | Auditoria: tabela gravada comparada com o recálculo pelo core | 0 | 0, na auditoria com 21524 eventos aplicados em 306 chaves |
| Tempo de recuperação | Do momento da morte do worker até todas as chaves dele terem novo dono | A definir depois da linha de base | 5750 ms (TF-01) |
| Latência de registro (p95) | k6 | A definir depois da linha de base | 8,8 e 9,5 ms sem falha; 30,0 ms com a queda de um worker. Uma rodada deu 6138 ms, sem causa conhecida |
| Latência de consulta (p95) | k6 | A definir depois da linha de base | 8,0 ms |
| Maior carga de um worker | Painel do cluster e endpoint do Gateway | Menor ou igual ao teto | Não medido no k3s |
| Falsos positivos do detector | Workers declarados mortos sem terem caído | 0 em rede normal | 0 com atraso de 300 ms até o RabbitMQ. 1 com atraso de 2,5 s até o Dispatcher (TF-02b) |

### 3.5 Calibração

Estes parâmetros têm valor inicial marcado como SUPOSIÇÃO. Cada um deve ser variado nos testes e o valor final deve ir para o relatório com a justificativa. Nas medições do k3s eles não foram variados: todas rodaram com os valores iniciais (relatório técnico, seção 9).

| Parâmetro | Valor inicial | O que observar ao variar |
|---|---|---|
| Folga do teto de carga | 25% sobre a média | Folga pequena equilibra mais e move mais chaves. Folga grande faz o contrário |
| Pontos virtuais por worker | 64 | Poucos pontos deixam a distribuição desigual |
| Intervalo do heartbeat | 1 s | Intervalo curto detecta antes e gasta mais rede |
| Tempo limite do heartbeat | 5 s | Limite curto gera falso positivo. Limite longo demora a recuperar |
| Limite de entregas antes da DLQ | 5 | Quantas tentativas valem a pena antes de isolar a mensagem |

### 3.6 Ambiente

- Testes TU, TA e TI-01: qualquer máquina com JDK 21 e Maven.
- Testes TC, TF e TM: k3s do grupo. Um nó com 4 CPUs e 8 GB de memória bastou para 3 workers, RabbitMQ, PostgreSQL, Gateway e interface (uma VM Multipass num MacBook Air, em 18/11/2026). A suposição deixa de ser suposição para essa configuração. Em outra máquina, a confirmação é do grupo.

### 3.7 Registro dos resultados

Os números do k3s já estão no relatório técnico (seção 8.3) e na apresentação, com data e configuração do cluster. Falta o grupo preencher a Ata 04 (revisão pós-testes): data, presença e o que decidiu a partir deles.
