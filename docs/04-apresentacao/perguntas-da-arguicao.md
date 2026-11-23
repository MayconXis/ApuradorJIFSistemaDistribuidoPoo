# Perguntas prováveis na arguição individual

Respostas curtas, do jeito que dá para falar em voz alta. Todas batem com o código e com o relatório técnico. Onde a resposta depende de algo que ainda não foi medido, ela diz isso.

Como a arguição é individual, todos precisam saber o básico de todas as partes. A coluna "Frente" segue a divisão sugerida no plano de trabalho (SUGESTÃO): infraestrutura e balanceamento com o Maycon, tolerância a falhas com o Felype, domínio e interface com a Maria Eduarda.

## 1. Visão geral

**Qual problema o sistema resolve?**
Calcula a classificação das chaves da fase classificatória do JIF pela regra de cada modalidade e mostra o critério que decidiu cada posição. Em SD, a parte nova é distribuir esse cálculo e mantê-lo correto quando um processo cai.

**Por que distribuir, se a conta é leve?**
Não é por desempenho. É por disponibilidade e consistência: se um worker cai, a apuração continua e nenhum resultado se perde ou é contado duas vezes. A rajada no fim da rodada é hipótese nossa, sem dado do JIF.

**O que é fato e o que é hipótese no trabalho?**
Fato: as regras das modalidades e o prazo de 2 horas para protesto (Art. 135). Hipóteses: a rajada de resultados, muita gente consultando ao mesmo tempo e o protesto aceito virar retificação. As leituras do regulamento que fizemos estão listadas como SUP-01 a SUP-08.

**O que já foi executado de verdade?**
O build com Maven dos sete módulos e os 78 testes do core e do cluster, no Mac em 17/11/2026 e de novo em um Linux em 23/11/2026, depois de o código ser reorganizado em camadas; o TI-02, com Dispatcher e dois workers de verdade (7 de 7, nas duas datas); e o sistema inteiro com o k6, primeiro fora do k3s, com quedas por `kill -9`, e depois no k3s, com o Chaos Mesh (TC-01, TC-02, TF-01, TF-02, TF-02b e TF-03, em 18/11/2026, antes da reorganização). O que não foi executado é o ensaio da demonstração (TM-01) e a implantação do zero pelo README (TM-02).

## 2. POO e o módulo core (frente: Maria Eduarda)

**Onde está a classe abstrata e por que ela é abstrata?**
`Modalidade`. Ela declara `pontosPara`, `criterios` e `placarDeWO` sem implementar, porque cada modalidade pontua e desempata do seu jeito. Não faz sentido criar "uma modalidade" genérica.

**Onde está a interface com várias implementações?**
`CriterioDesempate`, com 12 implementações: confronto direto, vitórias, saldo, marcados, sofridos, cartões vermelhos, amarelos, sanções, os três averages e sorteio.

**Onde aparece polimorfismo?**
No `Classificador`. Ele percorre a lista de critérios da modalidade e chama `comparar` em cada um sem saber qual critério é. Cada objeto responde com a sua regra.

**Onde aparece herança, e por que existe a `ModalidadeDeGols`?**
`Futsal` e `Handebol` estendem `ModalidadeDeGols`, que estende `Modalidade`. Futsal e handebol têm a mesma pontuação (3, 1, 0) e o mesmo WO pelo maior placar. A classe intermediária evita repetir esse código.

**Como você garante o encapsulamento?**
Atributos privados e nenhum setter público. A `Campanha` de uma equipe só muda pelo método `registrar`, que é de pacote: só a `Chave` chama, quando uma partida é registrada.

**Quais classes são imutáveis e por quê?**
`Placar`, `Posicao`, `Partida`, `Equipe` e `Disciplina`: classes `final`, campos `final`, sem setters. Uma posição calculada não deve mudar depois. O `Placar` copia o array de parciais na entrada e na saída para ninguém alterar por fora.

**Para que serve o `protected` em `recomecaQuandoOEmpateDiminui`?**
Isola a nossa leitura do regulamento (SUP-01) num método só. Por ser protegido, um teste cria uma subclasse que troca a leitura e mostra o resultado alternativo.

**Onde usam array, ArrayList, Collection e Map?**
Array: parciais dos sets (`int[][]`) e cartões por lado (`int[]`). ArrayList: listas de equipes e blocos de empate no `Classificador`. Collection: o parâmetro de jogos em `comparar`. Map: `Chave` guarda `Map<Equipe, Campanha>` e o catálogo guarda `Map<String, Modalidade>`.

**Como entra uma modalidade nova?**
Cria uma subclasse de `Modalidade` com a pontuação e a lista de critérios e registra no catálogo. O `Classificador` não muda. O teste TU-07 faz isso com uma modalidade inventada.

**Por que o core não usa Spring?**
Para os conceitos de POO ficarem visíveis e para os testes rodarem só com o JDK. Um teste confere que nenhuma classe do Spring está no caminho.

**Explique o empate triplo da capa.**
Três equipes com 3 pontos. Com três empatadas, vale a lista sem confronto direto: vitórias não separa, saldo separa o IFCE (+2). IFG e IFSP ficam em -1. Na nossa leitura a lista recomeça para as duas e vale o confronto direto: IFG venceu, fica em 2º. Se a lista não recomeçasse, gols marcados poria o IFSP em 2º.

**E se a comissão do JIF discordar da leitura?**
Troca uma linha: o retorno de `recomecaQuandoOEmpateDiminui`. O teste da outra leitura já existe.

## 3. Balanceamento (frente: Maycon)

**Qual é o algoritmo?**
Hash consistente com carga limitada. Workers e chaves ficam num anel. A chave vai ao primeiro worker adiante; se ele estiver no teto, vai ao próximo.

**O que é o teto?**
Arredonda para cima 1,25 vezes a média de chaves por worker. Os 25% são a folga, SUPOSIÇÃO a calibrar.

**Para que servem os pontos virtuais?**
Cada worker aparece em 64 pontos do anel. Com um ponto só, a divisão fica desigual por azar. No nosso teste em memória, a maior carga caiu de 27% acima da média para 14%.

**Por que não round-robin?**
Os resultados da mesma chave precisam ir sempre ao mesmo worker e em ordem. O round-robin espalharia os eventos da chave A por todos os workers, e dois recalculariam a mesma tabela ao mesmo tempo.

**O que acontece quando um worker sai do anel?**
Só as chaves dele mudam de dono. No teste com 30 chaves, saíram exatamente as 8 dele. As dos outros ficam onde estão.

**E quando um worker entra?**
Ele pega as chaves que agora caem primeiro nele, até o teto. Quem ficou acima do teto novo repassa as últimas chaves que recebeu.

**Por que não usar o plugin de hash consistente do RabbitMQ ou o HPA?**
O enunciado pede o algoritmo do grupo. E o plugin não tem o teto de carga nem fala com o nosso detector de falhas.

**Onde fica o anel?**
Na memória do Dispatcher, classe `AnelDeHashLimitado`, no módulo cluster, em Java puro. Por isso o Dispatcher roda com uma réplica só.

**O que é balanceado: mensagens ou chaves?**
Chaves. A carga de um worker é a quantidade de chaves que ele tem. Todas as mensagens de uma chave vão para o dono dela.

## 4. Tolerância a falhas (frente: Felype)

**Como uma falha é detectada?**
Heartbeat. Cada worker manda um a cada segundo. Sem heartbeat por 3 s, ele fica SUSPEITO. Por 5 s, MORTO, e só aí sai do anel. Os tempos são SUPOSIÇÃO.

**O detector pode errar?**
Pode: um worker lento pode ser declarado morto. A correção não depende dele, porque a trava da chave e a idempotência impedem resultado duplicado. O custo de um falso positivo é uma reatribuição à toa. No k3s medimos os dois lados: com 300 ms de atraso até o RabbitMQ (TF-02), nenhum worker saiu de VIVO. Com 2,5 s de atraso no heartbeat de um worker (TF-02b), ele foi declarado MORTO, 106 chaves mudaram de dono e mesmo assim 0 eventos se perderam e 0 foram aplicados duas vezes.

**Como as chaves são reatribuídas?**
O Dispatcher tira o worker do anel, o anel devolve as chaves órfãs com o dono novo, e o Dispatcher publica uma tarefa de reatribuição na fila de cada novo dono.

**Por que nenhum resultado se perde?**
O Gateway grava o evento como PENDENTE no PostgreSQL antes de responder 202. Se a publicação falhar, a varredura reenvia. Se o worker cair antes do ack, o RabbitMQ entrega de novo.

**Por que nenhum resultado é aplicado duas vezes?**
O worker marca o evento como APLICADO na mesma transação em que grava o resultado. Mensagem repetida não encontra nada pendente. E o `eventoId` é único no banco, então o mesmo envio duas vezes vira um evento só.

**O que a mensagem no RabbitMQ carrega?**
Só o id da chave. Ela avisa "esta chave tem eventos pendentes". Os dados ficam no banco. Por isso mensagem repetida ou atrasada não faz mal.

**E se dois workers pegarem a mesma chave ao mesmo tempo?**
Pode acontecer numa reatribuição, se o antigo voltar. A trava da linha da chave (`SELECT ... FOR UPDATE`) faz um esperar o outro. Testamos no PostgreSQL: a segunda transação esperou 1492 ms por uma trava de 1500 ms.

**E se chegar um resultado antigo depois de um novo?**
Os eventos têm número de sequência. Se a partida já tem um resultado com sequência maior, o antigo é marcado SUPERADO e não sobrescreve.

**Qual é o tempo de recuperação?**
Fora do k3s, com processos locais, medimos 6026 ms do `kill -9` até as 38 chaves do worker terem novo dono: 5 s de tempo limite mais até 1 s até a próxima verificação. No k3s, com o Chaos Mesh matando um worker, foram 5750 ms para as 19 chaves dele terem novo dono, e o pod voltou cerca de 10,4 s depois, já depois da reatribuição. Em outra execução, com o heartbeat atrasado (TF-02b), a decisão levou 9803 ms e, na anterior, 5059 ms. Não investigamos por que variou.

**O Gateway é ponto único de falha?**
É, e assumimos isso. Se cair, ninguém registra até ele voltar, mas nenhum evento aceito se perde. O k3s reinicia o pod, e o Dispatcher reconstrói o anel pelos heartbeats e pelas chaves gravadas. Eleição de líder fica como evolução. No k3s, apagamos o pod no meio da rajada: o pod novo ficou pronto em cerca de 10 s, 10,1% das requisições de registro falharam nesse intervalo e os 1839 eventos aceitos estavam todos no banco.

**O que acontece com uma mensagem que sempre falha?**
O worker devolve para a fila. Depois do limite de entregas, o RabbitMQ manda para a dead letter queue. Medimos que, com limite N, ela é entregue N + 1 vezes antes de sair.

**Para que servem as probes do k3s, se vocês têm o detector?**
A probe reinicia o pod que travou. O detector tira o worker do anel e reatribui as chaves. Uma complementa a outra.

## 5. Middleware e infraestrutura (frente: Maycon)

**Por que RabbitMQ e não Kafka?**
O RabbitMQ entrega o que precisamos com pouca operação: fila durável e replicada, ack manual, reentrega e DLQ. O Kafka é um log particionado, mais pesado de operar, e o histórico que ele daria já está no PostgreSQL.

**O que é fila quórum?**
Fila do RabbitMQ replicada entre nós por consenso (Raft). No nosso laboratório o RabbitMQ tem um nó só, então a fila não tem réplica. Está nas limitações.

**Por que uma fila por worker?**
A fila é a partição do worker. Com um consumidor e prefetch 1, a ordem dentro da partição se mantém.

**O que é ack manual e por que usar?**
O worker só confirma a mensagem depois de terminar a transação no banco. Se ele cair antes, o RabbitMQ entrega de novo.

**Por que k3s e não Docker Swarm?**
O k3s é um Kubernetes completo e leve. O Chaos Mesh, que usamos para injetar falhas, só roda em Kubernetes.

**Por que os workers são StatefulSet?**
Para cada pod ter nome estável (worker-0, worker-1, worker-2). Esse nome é o id do worker no anel e o nome da fila dele.

**Por que o Dispatcher usa estratégia Recreate?**
Para nunca existirem dois Dispatchers com dois anéis ao mesmo tempo durante uma troca de versão.

**Por que PostgreSQL?**
Guarda o histórico de eventos, que permite reconstruir qualquer chave, e faz a trava por chave sem precisar de outra peça.

**Para que serve o Spring Boot aqui?**
Spring AMQP para o RabbitMQ, Spring Data JPA para o banco e Actuator para as probes. Não usamos Spring Cloud LoadBalancer, Gateway nem Resilience4j no lugar dos algoritmos.

## 6. Interface e API (frente: Maria Eduarda)

**Como a tela se atualiza?**
Polling a cada 2 segundos, mandando a versão que já tem. Se nada mudou, o Gateway responde 304 sem corpo.

**Por que não SSE ou WebSocket?**
Cada tela aberta seguraria uma conexão no Dispatcher, que é uma réplica só, e o nginx e o Traefik precisariam de ajuste. Dois segundos de atraso não pesam diante de 2 horas de prazo para protesto.

**Com quem o Angular fala?**
Só com o Gateway, por REST. O nginx serve a interface e repassa `/api` para o Dispatcher.

**O que aparece no painel do cluster?**
Os workers, o estado do heartbeat (VIVO, SUSPEITO, MORTO), há quanto tempo veio o último sinal, as chaves de cada um e o teto de carga.

**O que o registro devolve?**
202 com o número do evento e o worker que recebeu a tarefa. A tela consulta o evento até ele sair de PENDENTE.

## 7. Testes

**Como vão provar zero perdidos e zero duplicatas?**
O k6 envia uma rajada com reenvios de propósito. No fim, espera a fila esvaziar e confere evento por evento no banco. Duplicatas vêm da auditoria, que recalcula cada chave do histórico e compara com a tabela gravada. Repetimos no k3s com o Chaos Mesh matando um worker, atrasando a rede e apagando o pod do Gateway.

**O zero do slide é resultado?**
A meta é zero. O resultado das rodadas que fizemos também foi zero: fora do k3s, 0 perdidos e 0 duplicatas com e sem a queda de um worker, e no k3s 0 eventos aceitos perdidos em todas as rodadas válidas, com a auditoria final sem duplicata nem chave divergente (21524 eventos aplicados). Isso é evidência dessas execuções, nesse ambiente. Poucas rodadas não provam que nunca acontece.

**Como testam os algoritmos sem RabbitMQ e sem banco?**
O módulo cluster depende de interfaces (`PublicadorDeTarefas`, `HistoricoDaChave`, `Relogio`). Nos testes elas são implementações em memória com relógio manual. No sistema real são as de Spring.

**O que vocês aprenderam rodando de verdade?**
Que cada JVM do Spring Boot levou cerca de 32 s para subir. Então um worker reiniciado sempre volta depois do tempo limite de 5 s, e o caminho que acontece é a reatribuição. E que, com o Gateway fora por cerca de 9 s, 14,7% das requisições foram recusadas, mas nenhum evento aceito se perdeu. No k3s aprendemos mais três coisas: o atraso do Chaos Mesh na primeira vez não pegou no tráfego pelo Service, o que só descobrimos medindo; uma rodada do TC-01 teve p95 de 6138 ms e não se repetiu; e uma repetição nossa reaproveitou os identificadores de evento e não testou nada, o que o k6 agora avisa.

**Como sabem que o atraso do Chaos Mesh pegou no tráfego?**
Medimos. De dentro de cada worker, abrimos uma conexão TCP até o RabbitMQ pelo nome do Service: 5 a 10 ms sem atraso e 233 a 406 ms com ele. Na primeira vez o atraso mirava o IP do pod e o Service passou direto, com 5 a 7 ms. O `describe` do Chaos Mesh dizia "Injected" nos dois casos, então só a medição vale.

**Por que o p95 do primeiro TC-01 no k3s foi de 6138 ms?**
Não sabemos. Foi a primeira rodada, o k6 deixou de iniciar 301 iterações e nenhum evento se perdeu. Nas duas repetições o p95 foi de 9,5 e 8,8 ms. Nossa hipótese é que o sistema estava frio, mas não a testamos.
