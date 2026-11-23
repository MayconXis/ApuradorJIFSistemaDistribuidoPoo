# Roteiro da demonstração ao vivo

Duração prevista: 4 a 5 minutos, logo depois da lâmina 11. O momento principal é matar um worker na frente do painel do cluster e mostrar as chaves dele mudando de dono.

**SUGESTÃO de papéis** (o grupo decide): Maria Eduarda opera a interface, Maycon digita os comandos no terminal, Felype narra o que acontece no painel.

## Antes da aula (no dia, 15 minutos antes)

| # | O que fazer | Comando ou lugar |
|---|---|---|
| 1 | Subir o sistema no k3s, se ainda não estiver de pé | `bash k8s/implantar.sh` |
| 2 | Conferir que os pods estão `Running` (postgres, rabbitmq, dispatcher, worker-0, worker-1, worker-2, frontend) | `kubectl -n apurador get pods` |
| 3 | Deixar o banco limpo, para a demonstração começar do zero | `bash k8s/remover.sh` e `bash k8s/implantar.sh` (apaga os dados) |
| 4 | Abrir a interface no navegador, com três abas: Registrar, Classificação e Cluster | `http://<ip-do-nó>/` |
| 5 | Deixar um terminal aberto, com fonte grande, no diretório do repositório | |
| 6 | Opcional: ligar um atraso de 3 s por tarefa, para dar tempo de matar o worker no meio do processamento. O comando reinicia os workers, então faça antes de abrir o painel | `kubectl -n apurador set env statefulset/worker WORKER_ATRASO_MS=3000` |
| 7 | Gravar um vídeo do ensaio, como plano C | Gravador de tela do sistema |

## Passo a passo

| # | Quem | Ação | O que a turma vê | O que falar (resumo) |
|---|---|---|---|---|
| 1 | Felype | Abrir a aba **Cluster** | 3 workers VIVO, 0 chaves | "Três workers vivos, mandando heartbeat a cada segundo. O anel ainda está vazio." |
| 2 | Maria Eduarda | Na aba **Registrar**, clicar em **Carregar exemplo do empate triplo** | Aviso "Exemplo carregado" | "Isso cria a chave da capa e registra os três placares: IFCE 4 x 1 IFG, IFG 3 x 1 IFSP, IFSP 4 x 3 IFCE." |
| 3 | Maria Eduarda | Abrir a aba **Classificação** | IFCE 1º por Saldo de gols, IFG 2º e IFSP 3º por Confronto direto | "Aqui está a resposta da capa. O IFCE passa direto pelo saldo. Entre IFG e IFSP, a lista recomeça e vale o confronto direto. Essa leitura é escolha nossa." |
| 4 | Maycon | Criar mais seis chaves | Terminal mostra "criada" seis vezes | `BASE=http://<ip-do-nó> bash scripts/demo/criar-chaves-demo.sh` |
| 5 | Felype | Voltar à aba **Cluster** | 7 chaves distribuídas entre os 3 workers, nenhum acima do teto | "Cada chave tem um dono. O teto é a média mais 25%." Apontar qual worker tem mais chaves. |
| 6 | Maycon | Matar o worker que tem mais chaves (troque `worker-1` pelo nome certo) | | `kubectl -n apurador delete pod worker-1 --grace-period=0 --force` |
| 7 | Felype | Olhar o painel por 6 segundos, sem falar por cima | O worker passa a SUSPEITO (cerca de 3 s), depois a MORTO (cerca de 5 s). As chaves dele aparecem nos outros. Em "Últimas mudanças no anel" aparece a linha da reatribuição | "Suspeito... morto. Só as chaves dele mudaram de dono. As dos outros ficaram onde estavam." |
| 8 | Maria Eduarda | Na aba **Registrar**, escolher uma chave que era do worker morto e registrar um placar | Aviso com o evento, o worker novo e a situação APLICADO | "O resultado foi para o novo dono e foi aplicado." |
| 9 | Felype | Voltar à aba **Cluster** e esperar o pod voltar (o StatefulSet recria com o mesmo nome) | O worker volta a VIVO e recebe chaves de novo | "O k3s recriou o pod. O detector viu o heartbeat e o anel rebalanceou." |
| 10 | Maria Eduarda | Na aba **Cluster**, clicar em **Conferir agora** | Pendentes 0, aplicados mais de uma vez 0, chaves com divergência 0 | "Isso vale para esta demonstração. Os números do teste de carga estão no relatório: no k3s, nenhum evento aceito se perdeu em nenhuma rodada válida." |
| 11 | Maria Eduarda | Retificação: registrar IFSP x IFCE como 4 x 4 na chave do exemplo e abrir **Classificação** | IFCE em 1º com 4 pontos | "Hipótese de uso: protesto aceito muda o placar e a chave é recalculada do histórico." |

Se o passo 7 não mostrar MORTO: o pod pode ter voltado antes dos 5 s. Nesse caso, explicar que a mensagem não confirmada volta para a fila do mesmo worker e que nada se perde. Para forçar a reatribuição, reduzir o StatefulSet: `kubectl -n apurador scale statefulset/worker --replicas=2` (o worker-2 sai e não volta até `--replicas=3`).

## Plano B: sem k3s, com docker compose

| Passo do roteiro | Comando equivalente |
|---|---|
| Subir | `docker compose up -d --build` e abrir `http://localhost:4200` |
| 4 | `BASE=http://localhost:4200 bash scripts/demo/criar-chaves-demo.sh` |
| 6 | `docker compose kill worker-1` |
| 9 | `docker compose start worker-1` |

## Plano C

Mostrar o vídeo gravado no ensaio ou as capturas do painel em `docs/04-apresentacao/telas/`: antes da queda, com o worker SUSPEITO e com o worker MORTO e as chaves dele nos outros. Elas foram tiradas com o sistema real rodando fora do k3s, em 18/11/2026.

## Depois da demonstração

Desligar o atraso artificial, se foi ligado: `kubectl -n apurador set env statefulset/worker WORKER_ATRASO_MS=0`.
