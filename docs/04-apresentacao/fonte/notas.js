// Notas do apresentador, uma por lâmina. A divisão da fala é: Maycon nas lâminas 1, 7 e 8
// (definido pelo grupo); Maria Eduarda e Felype nas demais (SUGESTÃO, a confirmar).
const N = [];

N.push(`QUEM FALA: Maycon. TEMPO: 45 s.

Boa noite, professor. Eu sou o Maycon, e comigo estão o Felype e a Maria Eduarda.
Em POO a gente propôs um sistema que apura a classificação das chaves do JIF, os Jogos das Instituições Federais. Olhem a chave ao lado: três equipes, três pontos cada. Quem passa direto para a semifinal depende de regras que mudam de modalidade para modalidade.
Em Sistemas Distribuídos a pergunta é outra: como fazer essa apuração rodar em vários processos e continuar certa quando um deles cai. É isso que vamos mostrar. A Maria Eduarda começa pelo problema.

(Os placares desta chave são fictícios. A explicação do desempate está no relatório, seção 2.2.)`);

N.push(`QUEM FALA: Maria Eduarda (sugestão). TEMPO: 1 min.

O JIF Goiás de 2026 foi a 12ª edição e reuniu cerca de mil estudantes dos 14 câmpus do IFG, em Goiânia, de 24 a 28 de agosto. A etapa nacional também é em Goiânia, de 18 a 23 de outubro, com 11 modalidades e o IFG como anfitrião.
O que é fato: a Comissão Técnica publica o boletim todo dia, em PDF. O regulamento nacional prevê informativos e boletins diários, nos Arts. 10 e 14, e cita sistema informático para inscrição e credenciamento, mas não cita sistema para apurar a classificação.
O que é inferência nossa: a classificação é feita à mão ou em planilha. Não achamos fonte que descreva o processo, então falta confirmar com a Educação Física do câmpus.`);

N.push(`QUEM FALA: Maria Eduarda (sugestão). TEMPO: 1 min 10 s.

Por que apurar é difícil? Porque cada modalidade tem a sua regra, e todas estão no regulamento nacional.
No futsal e no handebol, vitória vale 3, empate 1 e derrota 0. No vôlei não existe empate e os pontos dependem dos sets: 2 a 0 vale 3, 2 a 1 vale 2, e quem perde por 1 a 2 ainda leva 1.
O WO muda: no vôlei é fixo, 2 sets a 0 com parciais de 25 a 0. No futsal e no handebol, o placar do WO é o maior da fase dentro da chave, então depende dos outros jogos.
E o desempate: entre duas equipes, as três começam pelo confronto direto. Entre três ou mais, futsal e vôlei tiram o confronto direto. O handebol usa outra lista, de 13 critérios. E reparem que o futsal olha gols marcados antes de sofridos, e o handebol faz o contrário.
Isso tudo virou o módulo core, em Java puro, que é a parte de POO do trabalho.`);

N.push(`QUEM FALA: Maria Eduarda (sugestão). TEMPO: 1 min.

Por que distribuir? Vou separar o que é fato do que é hipótese.
Hipótese nossa: no fim de uma rodada, vários jogos terminam juntos e os resultados chegam em rajada. Logo depois, muita gente abre a tabela ao mesmo tempo. Não temos dado do JIF que prove isso. Os testes de carga vão reproduzir essa hipótese, não comprovar.
Fato: o regulamento dá 2 horas depois de uma partida coletiva para a instituição protestar, no Art. 135. A tabela precisa estar certa e explicada dentro dessa janela.
Proposta nossa: se o protesto for aceito e o placar mudar, o sistema retifica o resultado e recalcula a chave. O regulamento não diz qual é o efeito do protesto, então isso é hipótese de uso.
Por isso a meta não é velocidade. A conta de uma chave é leve. A meta é a tabela continuar disponível e correta mesmo quando um processo cai.`);

N.push(`QUEM FALA: Maria Eduarda (sugestão). TEMPO: 1 min 10 s.

Este é o caminho de um resultado, em seis passos.
Um: o mesário registra o placar na tela. Dois: o Gateway valida pela regra da modalidade e grava o evento no banco como pendente. Três: o Dispatcher pergunta ao anel quem é o worker dono daquela chave. Quatro: o RabbitMQ entrega um aviso na fila desse worker. Cinco: o worker trava a chave no banco, aplica os eventos pendentes e recalcula a tabela inteira. Seis: a tela mostra cada posição com o critério que decidiu.
O detalhe que segura tudo está em destaque: o Gateway só responde que aceitou depois de gravar no banco. Se a fila ou o worker falharem depois disso, o resultado não se perde, porque ele já está gravado. O Felype mostra a arquitetura.`);

N.push(`QUEM FALA: Felype (sugestão). TEMPO: 1 min.

Aqui estão as peças. Em verde, o código que a gente escreveu. Em cinza, a infraestrutura.
A interface é Angular e fala só com o Gateway, por REST. O Gateway e o Dispatcher são uma aplicação Spring Boot: ela grava o evento no PostgreSQL e publica um aviso no RabbitMQ. Cada worker tem a sua fila, e a fila é a partição dele. Os workers são Spring Boot com o core dentro: consomem a própria fila, aplicam os eventos e gravam a classificação.
A linha tracejada é o heartbeat: cada worker avisa o Dispatcher a cada segundo que está vivo. Tudo isso roda no k3s, que é a nossa VIM.
Uma limitação que a gente assume: o Dispatcher roda em uma réplica só, porque o anel fica em memória. Ele é ponto único de falha. Se cair, nenhum resultado aceito se perde, porque está no banco, e o k3s reinicia o pod. Eleição de líder fica como evolução.
Agora o Maycon explica as escolhas de infraestrutura.`);

N.push(`QUEM FALA: Maycon. TEMPO: 1 min 20 s.

Para cada peça, a escolha, o motivo e o que a gente descartou.
Middleware: RabbitMQ. Ele tem fila quórum, que é a fila durável e replicada, confirmação manual e dead letter queue para a mensagem que falha sempre. Descartamos o Kafka porque operar um log particionado é demais para o volume de uma competição. O histórico que o Kafka daria, a gente já tem no PostgreSQL.
VIM: k3s, porque é um Kubernetes completo e leve, roda em notebook, e o Chaos Mesh, que usamos para injetar falhas, só funciona em Kubernetes. Por isso não o Docker Swarm.
Backend em Spring Boot, porque o Spring AMQP, o Spring Data e o Actuator já resolvem a integração e o grupo conhece Java. Angular na interface, falando só com o Gateway. PostgreSQL guarda o histórico e faz a trava de cada chave, sem precisar de mais uma peça como o Redis. E k6 e Chaos Mesh para os testes, porque eles repetem o mesmo cenário quantas vezes for preciso.
Um ponto importante: o balanceamento e a tolerância a falhas não vêm de biblioteca. Não usamos o plugin de hash do RabbitMQ, o HPA, o Spring Cloud LoadBalancer nem o Resilience4j. Os dois algoritmos são nossos, e o próximo slide é o de balanceamento.`);

N.push(`QUEM FALA: Maycon. TEMPO: 1 min 30 s.

O balanceamento é hash consistente com carga limitada.
Workers e chaves ocupam posições num anel. Cada worker aparece em vários pontos, os pontos virtuais, para a divisão ficar uniforme. Passo um: a chave cai numa posição do anel. Passo dois: ela vai para o primeiro worker adiante. Passo três: se esse worker já está no teto de carga, ela pula para o próximo.
O teto é a média de chaves por worker mais 25% de folga. A folga é suposição nossa e vai ser calibrada nos testes.
Por que não round-robin? Porque os resultados da mesma chave precisam ir sempre para o mesmo worker e em ordem. O round-robin mandaria o primeiro resultado da chave A para o worker 0, o segundo para o worker 1, e dois workers recalculariam a mesma tabela ao mesmo tempo. No anel, a chave A vai sempre para o mesmo worker.
E quando um worker sai, só as chaves dele mudam de dono. No nosso teste em memória, com 30 chaves, saíram exatamente as 8 que eram dele. Esse número é de teste em memória, não do cluster.
A técnica é de Mirrokni, Thorup e Zadimoghaddam, de 2018. A implementação é nossa, em Java puro. O Felype segue com a tolerância a falhas.`);

N.push(`QUEM FALA: Felype (sugestão). TEMPO: 1 min 20 s.

A tolerância a falhas tem três passos, e esta linha do tempo mostra os três.
Detectar: cada worker manda heartbeat a cada segundo. Quando um worker cai, o Dispatcher conta o silêncio. Com 3 segundos ele vira suspeito, e o painel já mostra. Com 5 segundos ele é declarado morto. Esses tempos são suposição e vão ser calibrados.
Reatribuir: o Dispatcher tira o worker do anel. Só as chaves dele mudam de dono, e o novo dono recebe um aviso para cada uma.
Reprocessar: o novo dono trava a chave no banco e aplica o que estava pendente.
Embaixo, por que nada se perde e nada duplica. Não se perde porque o evento está no banco antes de ir para a fila. Não duplica porque o evento é marcado como aplicado na mesma transação que grava o resultado: ou acontecem as duas coisas, ou nenhuma. Se a mensagem chegar de novo, não tem mais nada pendente e ela é ignorada.`);

N.push(`QUEM FALA: Felype (sugestão). TEMPO: 1 min 10 s.

O zero é a meta, e nas rodadas que a gente mediu ele também foi o resultado: nenhum evento aceito se perdeu e a auditoria final não achou duplicata. Poucas rodadas não provam que nunca acontece.
A coluna do meio é fora do k3s. A da direita é no k3s, numa máquina virtual dentro de um MacBook Air, com o k6 rodando junto.
No k3s, a rajada deu zero perdidos. O p95 foi de uns 9 milissegundos em duas rodadas, e uma rodada deu 6 segundos, que a gente não sabe explicar. Matando um worker com o Chaos Mesh, as chaves dele tiveram novo dono em 5,8 segundos. O Gateway levou uns 10 segundos para voltar, e 10% das requisições falharam nesse intervalo, sem perder nenhum evento aceito.
Atrasar a rede em 300 milissegundos não derrubou ninguém. Atrasar o heartbeat de um worker em 2,5 segundos fez o detector declará-lo morto, o falso positivo que a gente previa, e mesmo assim deu zero perdidos.
Na primeira vez, o atraso nem chegou ao tráfego. A gente só viu porque mediu.`);

N.push(`QUEM FALA: Felype (sugestão). TEMPO: 1 min.

O que entregamos: o código em módulos, com o core de POO separado do Spring; os manifestos do k3s com a implantação do zero; os scripts do k6 e do Chaos Mesh; e a documentação: requisitos, atas, diagramas e relatório técnico.
O que precisa ser validado, e a gente deixa isso claro: a leitura do empate reduzido, que é escolha nossa e muda o 2º lugar do exemplo da capa; as duas hipóteses de uso, a rajada e a retificação depois do protesto; os parâmetros dos algoritmos; e o Gateway como ponto único de falha.
Obrigado. Agora a gente mostra o sistema funcionando e fica à disposição para perguntas.`);

module.exports = { notas: N };
