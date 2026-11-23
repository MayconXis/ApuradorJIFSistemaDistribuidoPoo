package br.edu.ifg.apurador.cluster.apuracao;

import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import br.edu.ifg.apurador.core.modalidade.Modalidade;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Algoritmo de tolerância a falhas do grupo, parte 3: reprocessamento idempotente.
 * É o que o worker faz quando recebe uma tarefa.
 *
 * Com a chave travada:
 * 1. lê os eventos PENDENTES da chave, em ordem de sequência;
 * 2. se não há nenhum, a mensagem era repetida: não faz nada;
 * 3. aplica cada evento à partida e marca como APLICADO;
 * 4. reconstrói a chave pelo histórico e recalcula a classificação com o core;
 * 5. grava as posições.
 * Tudo isso é confirmado junto. Se o worker cair no meio, o banco desfaz tudo e
 * os eventos continuam PENDENTES para o próximo worker.
 *
 * Por que isso é idempotente: o estado PENDENTE ou APLICADO de cada evento está
 * no banco, na mesma transação da tabela. Um evento já aplicado nunca é aplicado
 * de novo, não importa quantas vezes a mensagem chegue nem a qual worker.
 * E como a tabela é recalculada do histórico inteiro, os mesmos eventos dão
 * sempre a mesma Posicao.
 */
public class AplicadorDeEventos {

    private final HistoricoDaChave historico;
    private final CatalogoDeModalidades catalogo;
    private final String worker;
    private final ValidadorDeResultado validador = new ValidadorDeResultado();
    private final CalculoDaTabela calculo = new CalculoDaTabela();

    public AplicadorDeEventos(HistoricoDaChave historico, CatalogoDeModalidades catalogo, String worker) {
        this.historico = historico;
        this.catalogo = catalogo;
        this.worker = worker;
    }

    public ResumoDaApuracao processarChave(String chaveId) {
        return historico.comTrava(chaveId, transacao -> {
            List<EventoDeResultado> pendentes = transacao.pendentes();
            if (pendentes.isEmpty()) {
                return new ResumoDaApuracao(chaveId, 0, 0, 0);
            }
            DadosDaChave dados = transacao.chave();
            Modalidade modalidade = catalogo.buscar(dados.getModalidade());
            Map<String, PartidaGravada> partidas = new LinkedHashMap<>();
            for (PartidaGravada partida : transacao.partidas()) {
                partidas.put(partida.getId(), partida);
            }

            int aplicados = 0;
            int rejeitados = 0;
            int superados = 0;
            for (EventoDeResultado evento : pendentes) {
                PartidaGravada partida = partidas.get(evento.getPartidaId());
                String motivo = validador.validar(evento, partida, modalidade);
                if (motivo != null) {
                    transacao.encerrar(evento, EstadoDoEvento.REJEITADO, motivo, worker);
                    rejeitados++;
                } else if (partida.getSeqAplicada() > evento.getSeq()) {
                    transacao.encerrar(evento, EstadoDoEvento.SUPERADO,
                            "A partida já tem um resultado mais novo (seq " + partida.getSeqAplicada() + ").", worker);
                    superados++;
                } else {
                    EstadoDaPartida novoEstado = partida.temResultado() ? EstadoDaPartida.RETIFICADA : EstadoDaPartida.HOMOLOGADA;
                    transacao.aplicar(evento, novoEstado, worker);
                    partidas.put(partida.getId(), partida.comResultado(evento, novoEstado));
                    aplicados++;
                }
            }
            if (aplicados > 0) {
                transacao.gravarClassificacao(calculo.calcular(dados, partidas.values(), modalidade));
            }
            return new ResumoDaApuracao(chaveId, aplicados, rejeitados, superados);
        });
    }

    public String getWorker() {
        return worker;
    }
}
