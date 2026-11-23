# Proposta de solução: Apurador JIF

Atividade final de Sistemas Distribuídos, com o domínio da proposta de POO.
Equipe: Maycon Douglas da Silva, Felype de Moura Lino e Silva, Maria Eduarda Mendonça Fernandes. IFG Câmpus Inhumas.

## 1. Problema

Nos Jogos das Instituições Federais (JIF), a classificação de cada chave depende de regras que mudam de modalidade para modalidade: a pontuação, o placar do WO e a lista de critérios de desempate (Regulamento da Etapa Nacional 2026, Arts. 61, 62, 66, 67, 111 a 113). O regulamento cita sistema para inscrição e credenciamento, mas não cita sistema para apurar a classificação. A instituição tem até 2 horas depois de uma partida coletiva para protestar (Art. 135), então a tabela precisa sair rápido e explicar cada posição.

Propomos o **Apurador JIF**: um sistema distribuído que recebe os resultados, calcula a classificação da chave pela regra da modalidade e mostra o critério que decidiu cada posição. O cálculo é o mesmo modelo orientado a objetos que propusemos em POO. O que é novo é distribuir o processamento e mantê-lo correto quando um processo falha.

**Hipótese nossa, sem dado do JIF:** os resultados chegam em rajada no fim das rodadas e muita gente consulta a tabela ao mesmo tempo. O ganho que buscamos é disponibilidade e consistência, não desempenho: a conta em si é leve.

## 2. Arquitetura

| Peça | Papel |
|---|---|
| Interface (Angular) | Registrar resultado, ver a classificação com o critério decisivo e acompanhar o cluster. Fala só com o Gateway, por REST |
| Gateway / Dispatcher (Spring Boot) | Valida o resultado, grava o evento no PostgreSQL, escolhe o worker dono da chave e publica a tarefa na fila desse worker |
| RabbitMQ | Uma fila por worker (cada fila é uma partição), do tipo quórum, com confirmação manual e dead letter exchange |
| Workers (Spring Boot) | Consomem a própria fila, aplicam os eventos pendentes da chave de forma idempotente e recalculam a classificação com o core |
| PostgreSQL | Histórico de eventos, partidas homologadas e posições |
| Core (Java puro) | Domínio de POO: `Modalidade`, `CriterioDesempate`, `Classificador`. Sem Spring |

O diagrama de componentes e as duas sequências estão em `docs/diagramas`.

## 3. Middleware: RabbitMQ

Um resultado é um evento: alguém registra um jogo e o sistema reage. Com uma fila no meio, o Gateway não depende de o worker estar de pé naquele instante. Escolhemos o RabbitMQ porque ele entrega o que o problema pede com pouca operação: fila durável e replicada (quórum), confirmação manual do consumidor, reentrega quando o consumidor cai e dead letter exchange para a mensagem que falha repetidas vezes.

Alternativas descartadas: **Kafka**, porque o volume de uma competição não justifica operar um log particionado, e o histórico no PostgreSQL já permite reprocessar uma chave do zero. **gRPC**, porque é chamada direta e acopla o Gateway à disponibilidade do worker.

## 4. VIM: k3s

O k3s é um Kubernetes completo em um binário, leve o bastante para rodar em notebook. Usamos Deployments e StatefulSets, Services, probes do Actuator e o Ingress que já vem com ele. Descartamos o Docker Swarm porque o Chaos Mesh, que usamos para injetar falhas, só atua em Kubernetes, e o OpenStack porque é pesado demais para o nosso hardware.

Os recursos nativos entram só como complemento. Não usamos o HPA, o plugin de hash consistente do RabbitMQ, o Spring Cloud LoadBalancer nem o Resilience4j no lugar dos algoritmos abaixo.

## 5. Algoritmo de balanceamento: hash consistente com carga limitada

Workers e chaves ocupam posições em um anel, cada worker com vários pontos virtuais. A chave vai ao primeiro worker adiante no anel. Se esse worker já está no teto de carga (a média de chaves por worker mais uma folga), a chave pula para o próximo. Quando um worker sai, só as chaves dele mudam de dono.

Por que não round-robin: os resultados de uma mesma chave precisam ir sempre ao mesmo worker e em ordem, para que um único processo por vez recalcule aquela tabela. O round-robin espalha os eventos da mesma chave por todos os workers. O hash consistente dá essa afinidade, e o teto evita que um worker fique sobrecarregado.

A técnica é conhecida (Mirrokni, Thorup e Zadimoghaddam, 2018). A implementação é nossa, em Java puro. A folga do teto começa em 25% e será calibrada nos testes.

## 6. Algoritmo de tolerância a falhas: heartbeat, reatribuição e reprocessamento idempotente

1. **Detectar.** Cada worker envia um heartbeat ao Dispatcher em intervalo fixo. Sem heartbeat dentro do tempo limite, o worker é declarado morto.
2. **Reatribuir.** O Dispatcher remove o worker do anel e entrega as chaves dele aos próximos workers, respeitando o teto.
3. **Reprocessar.** O evento já está gravado no PostgreSQL antes de ser publicado. O novo dono trava a chave, aplica os eventos pendentes em ordem, reconstrói a chave pelo histórico e recalcula. Um evento já aplicado não é aplicado de novo: mesma entrada, mesma `Posicao`.

Confirmação manual, dead letter exchange e probes do k3s complementam, mas não substituem, esses três passos.

**Limitação assumida:** o Gateway, que guarda o anel em memória, é ponto único de falha. Ele roda em uma réplica, com probes e reinício automático, e reconstrói o anel ao voltar. Nenhum evento aceito se perde, porque já está no banco. Eleição de líder fica como evolução. O PostgreSQL também é uma instância só.

## 7. Escopo

**Entra no MVP:** fase classificatória em chaves; futsal, voleibol e handebol; registro de resultado e de WO; classificação com critério decisivo; retificação de resultado (hipótese de uso para protesto aceito, já que o regulamento não define o efeito do protesto); painel do cluster.

**Fica de fora:** modalidades individuais, mata-mata, login e autenticação.

## 8. Como vamos verificar

Testes unitários do core, incluindo o empate triplo no futsal (IFCE, IFG e IFSP com 3 pontos). Testes dos dois algoritmos sem infraestrutura. Carga com k6 (rajada de resultados e consultas simultâneas). Falhas com Chaos Mesh (matar um worker no meio do processamento e atrasar mensagens). Meta: zero eventos perdidos e zero duplicatas na tabela final. Os números só entram no relatório depois de medidos no nosso cluster.

## Referências

- Regulamento JIF Etapa Nacional 2026. COJIF, Goiânia, 12/06/2026.
- MIRROKNI, V.; THORUP, M.; ZADIMOGHADDAM, M. Consistent Hashing with Bounded Loads. SODA 2018. arXiv:1608.01350.
- KARGER, D. et al. Consistent Hashing and Random Trees. STOC 1997.
- CHANDRA, T. D.; TOUEG, S. Unreliable Failure Detectors for Reliable Distributed Systems. Journal of the ACM, 1996.
- COULOURIS, G. et al. Sistemas Distribuídos: Conceitos e Projeto, cap. 6 e 18. TANENBAUM, A.; VAN STEEN, M. Sistemas Distribuídos, cap. 4 e 8.
- Documentação do RabbitMQ (quorum queues, consumer acknowledgements, dead letter exchanges), do Kubernetes (probes) e do Chaos Mesh.
