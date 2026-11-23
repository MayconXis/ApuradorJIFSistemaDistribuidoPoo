# Especificação complementar de requisitos: Apurador JIF

Trabalho final de Programação Orientada a Objetos e de Sistemas Distribuídos.
IFG Câmpus Inhumas, Engenharia de Software.
Equipe: Maycon Douglas da Silva, Felype de Moura Lino e Silva, Maria Eduarda Mendonça Fernandes.

O documento principal de requisitos é `documento-de-requisitos.odt`, que segue o modelo da disciplina de Análise e Projeto de Sistemas (domínio, objetivos, atas, protótipo, artefatos RIHM, RIHW, RISW, RnF e RFn, casos de uso documentados e diagramas).

Este arquivo complementa aquele documento com o que o modelo da disciplina não pede: as regras de negócio com o artigo do regulamento, as lacunas do regulamento, a prioridade MoSCoW e os critérios de aceitação em Gherkin. A estrutura segue a ISO/IEC/IEEE 29148.

Correspondência dos identificadores: RF-01 aqui é o RFn - 001 do documento principal, RF-02 é o RFn - 002, e assim por diante. RNF-01 é o RnF - 001. Prioridade M corresponde a Imprescindível. Prioridades S e C correspondem a Desejável.

Convenções deste documento:

- **SUPOSIÇÃO**: algo que assumimos por falta de informação e que precisa ser validado.
- **HIPÓTESE**: afirmação nossa sobre o uso do sistema, ainda sem dado que a sustente.
- **A MEDIR**: valor que só pode ser preenchido depois de uma execução real dos testes. Os valores do k3s já foram medidos em 18/11/2026 e estão no relatório técnico, seção 8.3.
- Prioridade MoSCoW: M (Must, obrigatório), S (Should, importante), C (Could, desejável), W (Won't, fora desta entrega).

## 1. Introdução

### 1.1 Propósito

Este documento define o que o Apurador JIF deve fazer e com que qualidade. Ele serve de base para o projeto, para os testes e para a arguição nas duas disciplinas.

### 1.2 Escopo

O Apurador JIF recebe os resultados das partidas de uma chave dos Jogos das Instituições Federais (JIF), calcula a classificação da chave pelas regras da modalidade e mostra, em cada posição, o critério que decidiu a colocação.

O sistema é distribuído: os resultados entram por um Gateway, são distribuídos entre vários workers por um algoritmo de balanceamento escrito pelo grupo e continuam sendo processados quando um worker cai, por um algoritmo de tolerância a falhas também escrito pelo grupo.

Entram no MVP: fase classificatória em chaves, nas modalidades futsal, voleibol e handebol.

Ficam fora desta entrega: modalidades individuais, mata-mata (repescagem, semifinais e finais), login e autenticação.

### 1.3 Visão geral do produto

**Perspectiva.** O sistema não substitui a súmula nem o boletim técnico. Ele automatiza a conta da classificação, que hoje, pelo que conseguimos apurar, não tem sistema citado no regulamento (HIPÓTESE: a conta é feita à mão ou em planilha).

**Funções principais.** Cadastrar chave, registrar resultado, classificar com desempate, consultar a tabela, acompanhar o estado do cluster.

**Usuários (atores).**

| Ator | Quem é | O que faz no sistema |
|---|---|---|
| Operador da apuração | Membro da comissão técnica ou mesário | Cadastra a chave e registra os resultados |
| Consumidor da tabela | Estudante, técnico, chefe de delegação | Consulta a classificação e o critério decisivo |
| Administrador do cluster | Integrante do grupo, na demonstração | Acompanha workers e heartbeats, derruba um worker, confere a reatribuição |
| Ferramentas de teste | k6 e Chaos Mesh | Geram carga e injetam falhas |

**Limitações conhecidas.**

- O Gateway, que contém o Dispatcher, é ponto único de falha. Ele é reiniciado pelo k3s, mas não tem eleição de líder.
- O PostgreSQL roda em uma instância só.
- O sorteio, último critério de todas as listas, não é feito pelo sistema. O sistema só avisa que a posição depende dele.

### 1.4 Definições

| Termo | Significado |
|---|---|
| Chave | Grupo de equipes que jogam entre si em rodízio simples |
| Partida | Um jogo entre duas equipes da chave |
| Súmula | Registro oficial do jogo. No sistema, é o resultado enviado pelo operador |
| Evento | Um resultado enviado ao sistema, com identificador único |
| Campanha | A linha de uma equipe na tabela da chave |
| Critério decisivo | O critério que separou a equipe das que estavam empatadas com ela |
| WO | Vitória por ausência do adversário |
| Worker | Processo que aplica os eventos e recalcula a classificação |
| Dispatcher | Parte do Gateway que decide qual worker cuida de cada chave |
| Anel | Estrutura do hash consistente: chaves e workers ocupam posições em um círculo |
| Teto de carga | Número máximo de chaves que um worker pode receber |
| Heartbeat | Sinal periódico que o worker envia para avisar que está vivo |
| DLQ | Fila para onde vão as mensagens que falharam várias vezes |

## 2. Referências

1. Regulamento JIF Etapa Nacional 2026. COJIF, Goiânia, 12/06/2026. Disponível em portal.redefederal.org.br/images/Documentos/regulamento-etapa-nacional.pdf. Artigos conferidos no texto original em 16/11/2026.
2. ISO/IEC/IEEE 29148:2018. Systems and software engineering, Life cycle processes, Requirements engineering.
3. Enunciado da atividade final de Sistemas Distribuídos (requisitos obrigatórios 1 a 4).
4. Proposta de aplicação apresentada em POO I (deck "Apuração de competições do JIF").

## 3. Regras de negócio extraídas do regulamento

Cada regra cita o artigo de origem. A redação abaixo é um resumo fiel, não uma transcrição.

| ID | Regra | Origem |
|---|---|---|
| RN-01 | Com seis equipes, a competição coletiva tem duas chaves de três, em rodízio simples. O 1º de cada chave vai direto à semifinal. O 2º enfrenta o 3º da chave oposta na repescagem | Art. 28 |
| RN-02 | Com três, quatro ou cinco equipes, a competição é em rodízio simples | Art. 29 |
| RN-03 | Futsal: vitória 3 pontos, empate 1, derrota 0 | Art. 61 |
| RN-04 | Futsal, WO: o placar do vencedor é o maior da fase dentro da chave | Art. 61, parágrafo único |
| RN-05 | Futsal, empate entre duas equipes, nesta ordem: confronto direto, vitórias, saldo de gols, gols marcados, gols sofridos, cartões vermelhos, cartões amarelos, sorteio | Art. 62, § 1º |
| RN-06 | Futsal, empate entre três ou mais: os mesmos critérios, sem o confronto direto | Art. 62, § 2º |
| RN-07 | Handebol: vitória 3 pontos, empate 1, derrota 0. WO com o maior placar da fase dentro da chave | Art. 66 e parágrafo único |
| RN-08 | Handebol, empate entre duas equipes, nesta ordem: confronto direto, vitórias, saldo de gols na fase, gols sofridos, gols marcados, gol average, sanções, sorteio | Art. 67, § 1º |
| RN-09 | Handebol, empate entre três ou mais, nesta ordem: vitórias; pontos, saldo, gols sofridos e gols marcados no confronto direto entre as empatadas; saldo, gols sofridos e gols marcados na fase; gol average no confronto direto; gol average na fase; sanções no confronto direto; sanções entre as empatadas; sorteio | Art. 67, § 2º |
| RN-10 | Handebol, pontuação das sanções: cartão amarelo 1, exclusão de 2 minutos 2, desqualificação pelo terceiro 2 minutos 6, desqualificação direta 10, desqualificação com cartão azul 15. Menor pontuação é melhor | Art. 67 (tabela de sanções) |
| RN-11 | Voleibol, fase classificatória: melhor de 3 sets. Os dois primeiros vão a 25 pontos e o terceiro a 15 | Art. 111 |
| RN-12 | Voleibol: vitória por 2x0 vale 3 pontos, vitória por 2x1 vale 2, derrota por 1x2 vale 1, derrota por 0x2 vale 0 | Art. 112 |
| RN-13 | Voleibol, WO: o vencedor marca 3 pontos, com 2 sets a 0 e parciais de 25x0 | Art. 112, parágrafo único |
| RN-14 | Voleibol, empate entre duas equipes: confronto direto, vitórias, sets average, pontos average, sorteio. Entre três ou mais: a mesma lista, sem o confronto direto | Art. 113, §§ 1º e 2º |
| RN-15 | Protesto em modalidade coletiva: até 2 horas após o término da partida | Art. 135, parágrafo único |

### 3.1 O que o regulamento não define

Estes pontos não estão no regulamento. Cada um virou uma SUPOSIÇÃO, documentada e isolada em um único lugar do código.

| ID | Lacuna | O que adotamos |
|---|---|---|
| SUP-01 | Empate de três equipes que, depois de um critério, se reduz a duas: o confronto direto volta? | Volta. A lista de critérios recomeça para o grupo menor. Fica em um único método do Classificador |
| SUP-02 | "Maior placar da fase dentro da chave", no WO | O jogo com a maior diferença de gols entre os que tiveram vencedor. Sem nenhum jogo assim, 1 a 0 |
| SUP-03 | Fórmula dos averages | Pró dividido por contra. Quem tem zero contra fica à frente de qualquer average finito |
| SUP-04 | No futsal, saldo e gols contam quais jogos? | Todos os jogos da fase dentro da chave |
| SUP-05 | Handebol, critério XII, "sanções entre as equipes empatadas" | Sanções das equipes empatadas em todos os jogos da fase |
| SUP-06 | Sorteio | É feito fora do sistema. O sistema marca a posição como pendente de sorteio |
| SUP-07 | Estados de uma partida e homologação de resultado | O regulamento só fala em homologação de inscrições (Art. 14, II). Os estados da partida são modelo nosso |
| SUP-08 | Efeito de um protesto aceito sobre a classificação | O regulamento não define (Art. 135). Tratamos a retificação e o recálculo como HIPÓTESE de uso, não como regra |

## 4. Requisitos funcionais

| ID | Requisito | Prioridade | Regras ligadas |
|---|---|---|---|
| RF-01 | O sistema deve permitir cadastrar uma chave com nome, modalidade e de 3 a 5 equipes, e deve gerar as partidas em rodízio simples | M | RN-01, RN-02 |
| RF-02 | O sistema deve permitir registrar o resultado de uma partida: placar, parciais dos sets no voleibol, cartões no futsal e pontos de sanção no handebol | M | RN-03, RN-07, RN-12 |
| RF-03 | O sistema deve validar o resultado pela regra da modalidade antes de aceitá-lo e deve recusar o que for inválido, informando o motivo | M | RN-11, RN-12 |
| RF-04 | O sistema deve permitir registrar um WO, indicando a equipe ausente, e deve calcular o placar do WO pela regra da modalidade | M | RN-04, RN-07, RN-13 |
| RF-05 | O sistema deve calcular a pontuação de cada equipe e ordenar a chave por pontos | M | RN-03, RN-07, RN-12 |
| RF-06 | O sistema deve desempatar equipes com a mesma pontuação aplicando os critérios da modalidade, na ordem do regulamento, de acordo com o número de equipes empatadas | M | RN-05, RN-06, RN-08, RN-09, RN-14 |
| RF-07 | O sistema deve informar, em cada posição da classificação, o critério que decidiu a colocação | M | RN-15 |
| RF-08 | O sistema deve sinalizar a posição que só pode ser decidida por sorteio | S | SUP-06 |
| RF-09 | O sistema deve permitir consultar a classificação atual de uma chave | M | |
| RF-10 | O sistema deve permitir retificar o resultado de uma partida já registrada e deve recalcular a classificação (HIPÓTESE de uso para protesto aceito) | S | RN-15, SUP-08 |
| RF-11 | O sistema deve permitir consultar o andamento de um evento: pendente, aplicado ou rejeitado | S | |
| RF-12 | O sistema deve exibir um painel do cluster com os workers, o estado de cada um pelo heartbeat, as chaves atribuídas a cada worker e o teto de carga | M | |
| RF-13 | O sistema deve permitir listar as chaves e as partidas de uma chave | M | |
| RF-14 | O sistema deve isolar em uma fila separada a mensagem que falhar repetidas vezes, sem travar as demais | C | |
| RF-15 | O sistema deve permitir auditar uma chave, recalculando a classificação a partir do histórico e comparando com a tabela gravada | S | |

Fora desta entrega (W): modalidades individuais, mata-mata, login e autenticação, sorteio dentro do sistema, suspensão automática por cartões (Art. 63).

## 5. Requisitos não funcionais

| ID | Categoria | Requisito | Prioridade | Como se verifica |
|---|---|---|---|---|
| RNF-01 | Disponibilidade | A queda de um worker não pode interromper o registro nem a consulta. As chaves do worker caído devem passar a outro worker sem ação manual. Tempo de recuperação: linha de base de 5750 ms no k3s, medida em 18/11/2026 (relatório técnico, seção 8.3). O limite aceitável é decisão do grupo | M | TA-03, TI-01, TF-01 |
| RNF-02 | Durabilidade | Um resultado aceito pelo Gateway (HTTP 202) deve estar gravado no PostgreSQL antes da resposta. Meta: zero eventos perdidos | M | TI-01, TC-01, TF-01 |
| RNF-03 | Consistência e idempotência | Cada evento deve ser aplicado à tabela exatamente uma vez, mesmo que a mensagem seja entregue mais de uma vez. O mesmo conjunto de eventos deve sempre produzir a mesma classificação. Meta: zero duplicatas | M | TA-04, TI-01, TF-01 |
| RNF-04 | Ordenação por chave | Os eventos de uma mesma chave devem ser aplicados na ordem de sequência e por um único worker por vez | M | TA-04, TI-01 |
| RNF-05 | Balanceamento | A distribuição das chaves entre os workers deve usar hash consistente com carga limitada, implementado pelo grupo. Nenhum worker pode ficar acima do teto de carga. O teto deve ser calibrado nos testes e documentado | M | TA-01, TC-01 |
| RNF-06 | Tolerância a falhas | A detecção de falha deve usar heartbeat com tempo limite, implementado pelo grupo. Confirmação manual, DLQ e probes do k3s entram só como complemento | M | TA-02, TA-03, TF-01 |
| RNF-07 | Escalabilidade | A entrada ou a saída de um worker deve mover só as chaves necessárias, sem redistribuir todas | S | TA-01 |
| RNF-08 | Observabilidade | Cada serviço deve expor saúde e métricas pelo Actuator, e os registros de log devem trazer o identificador do evento | S | TM-01 |
| RNF-09 | Reprodutibilidade | A implantação no k3s deve ser reproduzível do zero seguindo o README, só com os manifestos do repositório | M | TM-02 |
| RNF-10 | Extensibilidade | Incluir uma nova modalidade não pode exigir alteração no Classificador | M | TU-07 |
| RNF-11 | Testabilidade | Os testes do core devem rodar sem Spring, sem rede e sem banco | M | TU-01 a TU-07 |
| RNF-12 | Desempenho | Latência de registro e de consulta sob rajada: limites definidos depois de medir a linha de base. Linha de base no k3s, medida em 18/11/2026: p95 de registro de 8,8 e 9,5 ms sem falha e p95 de consulta de 8,0 ms (relatório técnico, seção 8.3). Os limites são decisão do grupo | S | TC-01, TC-02 |
| RNF-13 | Restrições de tecnologia | Spring Boot, Angular, RabbitMQ, PostgreSQL e k3s. Não usar Spring Cloud LoadBalancer, Spring Cloud Gateway, Resilience4j, o plugin de hash consistente do RabbitMQ nem o HPA no lugar dos algoritmos do grupo | M | Revisão de código |
| RNF-14 | Recuperação do Gateway | Se o Gateway cair, nenhum evento já aceito pode ser perdido. Ao voltar, ele deve reconstruir o anel e reenviar os eventos pendentes | S | TA-03, TF-03 |

A hipótese que motiva RNF-01 e RNF-12 é nossa: os resultados chegam em rajada no fim das rodadas e muita gente consulta a tabela ao mesmo tempo. Não temos dado do JIF que confirme isso. HIPÓTESE.

## 6. Interfaces externas

- **Interface com o usuário**: aplicação web em Angular, com três telas: registro de resultado, classificação da chave e painel do cluster.
- **Interface de software**: API REST do Gateway, em JSON. O Angular fala só com o Gateway.
- **Middleware**: RabbitMQ, com uma fila por worker (cada fila é uma partição), filas do tipo quórum, confirmação manual e dead letter exchange.
- **Persistência**: PostgreSQL, com o histórico de eventos, as partidas homologadas e as posições.
- **Infraestrutura**: k3s. Chaos Mesh e k6 nos testes.

## 7. Critérios de aceitação

Os cenários abaixo cobrem os requisitos principais. O identificador do teste automatizado correspondente está na matriz de rastreabilidade.

```gherkin
# language: pt

Funcionalidade: Classificação com desempate (RF-05, RF-06, RF-07)

  Cenário: Empate triplo no futsal, com volta ao confronto direto
    Dado uma chave de futsal com as equipes IFCE, IFG e IFSP
    E os resultados "IFCE 4 x 1 IFG", "IFG 3 x 1 IFSP" e "IFSP 4 x 3 IFCE"
    Quando a classificação é calculada
    Então as três equipes têm 3 pontos
    E o IFCE fica em 1º, decidido por "Saldo de gols"
    E o IFG fica em 2º, decidido por "Confronto direto"
    E o IFSP fica em 3º

  Cenário: Voleibol pontua pelo placar em sets
    Dado uma chave de voleibol
    Quando uma equipe vence por 2 sets a 1
    Então a vencedora soma 2 pontos
    E a perdedora soma 1 ponto

  Cenário: Empate que só o sorteio resolve
    Dado duas equipes de futsal empatadas em todos os critérios numéricos
    Quando a classificação é calculada
    Então as duas posições ficam marcadas como pendentes de sorteio

Funcionalidade: Registro de resultado (RF-02, RF-03, RF-04)

  Cenário: Resultado inválido é recusado
    Dado uma partida de voleibol da fase classificatória
    Quando o operador envia o placar de 1 set a 1
    Então o Gateway responde com erro de validação
    E nenhum evento é gravado

  Cenário: WO no futsal usa o maior placar da chave
    Dado uma chave de futsal em que o jogo de maior diferença terminou 5 a 1
    Quando o operador registra um WO
    Então o vencedor do WO recebe 3 pontos
    E o placar considerado para o vencedor é 5 a 1

Funcionalidade: Idempotência (RNF-03)

  Cenário: A mesma mensagem é entregue duas vezes
    Dado um evento já aplicado à tabela da chave
    Quando a mesma mensagem chega de novo a qualquer worker
    Então a tabela não muda
    E o evento continua com uma única aplicação registrada

Funcionalidade: Balanceamento com teto de carga (RNF-05, RNF-07)

  Cenário: Nenhum worker passa do teto
    Dado 3 workers no anel e uma folga de 25% sobre a média
    Quando 30 chaves são atribuídas
    Então nenhum worker tem mais chaves do que o teto

  Cenário: A saída de um worker move só as chaves dele
    Dado 30 chaves distribuídas entre 3 workers
    Quando um worker é removido do anel
    Então só as chaves que eram dele mudam de dono

Funcionalidade: Tolerância a falhas (RNF-01, RNF-02, RNF-06)

  Cenário: Worker cai no meio do processamento
    Dado uma rajada de resultados em processamento por 3 workers
    Quando um worker para de enviar heartbeat por mais tempo que o limite
    Então o Dispatcher declara o worker morto
    E remove o worker do anel
    E as chaves dele passam a outros workers
    E nenhum evento é perdido
    E nenhum evento é aplicado duas vezes
    E a classificação final é igual à calculada direto pelo core

Funcionalidade: Extensibilidade (RNF-10)

  Cenário: Nova modalidade sem alterar o Classificador
    Dado uma nova subclasse de Modalidade com seus critérios
    Quando o Classificador recebe uma chave dessa modalidade
    Então a classificação é calculada sem nenhuma mudança no Classificador

Funcionalidade: Retificação (RF-10)

  Cenário: Resultado corrigido depois de registrado
    Dado uma partida com resultado já aplicado
    Quando o operador envia a retificação do placar
    Então a classificação é recalculada com o novo placar
    E a partida fica marcada como retificada
```

## 8. Verificação

| Tipo | O que cobre | Onde roda |
|---|---|---|
| Testes unitários do core (TU) | Regras de negócio, desempate, imutabilidade, extensibilidade | JUnit, sem infraestrutura |
| Testes dos algoritmos (TA) | Anel com teto de carga, detector de falhas, coordenação, aplicação idempotente | JUnit, com relógio manual |
| Integração simulada (TI-01) | Rajada, morte de worker, reentrega, comparação com o core | JUnit, com fila e histórico em memória |
| Integração com infraestrutura (TI-02) | Fluxo completo com RabbitMQ e PostgreSQL reais | Spring Boot Test com Testcontainers |
| Carga (TC) | Rajada de resultados e consultas simultâneas | k6 contra o k3s |
| Injeção de falhas (TF) | Morte de worker, atraso de rede, queda do Gateway | Chaos Mesh e kubectl no k3s |
| Manual (TM) | Interface e implantação do zero | Roteiro da demonstração e README |

## 9. Suposições e dependências

- A leitura do empate reduzido (SUP-01) precisa ser confirmada com a comissão técnica.
- O grupo consegue rodar um k3s com recursos para 3 workers, RabbitMQ, PostgreSQL, Gateway e interface. SUPOSIÇÃO.
- Os parâmetros dos algoritmos (intervalo do heartbeat, tempo limite, folga do teto, pontos virtuais) têm valores iniciais marcados como SUPOSIÇÃO e serão calibrados nos testes.
- O professor aceitou o reaproveitamento do domínio de POO, conforme informado pelo grupo.
