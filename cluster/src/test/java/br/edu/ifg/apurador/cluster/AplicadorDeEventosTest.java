package br.edu.ifg.apurador.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifg.apurador.cluster.apoio.HistoricoEmMemoria;
import br.edu.ifg.apurador.cluster.apoio.RelogioManual;
import br.edu.ifg.apurador.cluster.apuracao.AplicadorDeEventos;
import br.edu.ifg.apurador.cluster.apuracao.EstadoDaPartida;
import br.edu.ifg.apurador.cluster.apuracao.EstadoDoEvento;
import br.edu.ifg.apurador.cluster.apuracao.EventoDeResultado;
import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import br.edu.ifg.apurador.cluster.apuracao.ResumoDaApuracao;
import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import br.edu.ifg.apurador.core.sumula.Lado;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TA-04. Algoritmo de tolerância a falhas, parte 3: aplicação idempotente e em ordem (RNF-03, RNF-04, RF-10). */
@DisplayName("TA-04 Tolerância a falhas: reprocessamento idempotente e em ordem")
class AplicadorDeEventosTest {

    private HistoricoEmMemoria historico;
    private AplicadorDeEventos aplicador;
    private List<String> jogos;

    @BeforeEach
    void preparar() {
        historico = new HistoricoEmMemoria(new RelogioManual());
        aplicador = new AplicadorDeEventos(historico, CatalogoDeModalidades.padrao(), "worker-0");
        // J1 = IFCE x IFG, J2 = IFCE x IFSP, J3 = IFG x IFSP
        jogos = historico.criarChave("A", "Chave A", "FUTSAL", Arrays.asList("IFCE", "IFG", "IFSP"));
    }

    private void empateTriplo() {
        historico.gravarPendente(EventoDeResultado.gols("e1", "A", jogos.get(0), 4, 1)); // IFCE 4 x 1 IFG
        historico.gravarPendente(EventoDeResultado.gols("e2", "A", jogos.get(2), 3, 1)); // IFG 3 x 1 IFSP
        historico.gravarPendente(EventoDeResultado.gols("e3", "A", jogos.get(1), 3, 4)); // IFCE 3 x 4 IFSP
    }

    private static List<String> ordem(List<LinhaDaTabela> tabela) {
        List<String> nomes = new ArrayList<>();
        for (LinhaDaTabela linha : tabela) {
            nomes.add(linha.getEquipe() + ":" + linha.getCriterioDecisivo());
        }
        return nomes;
    }

    @Test
    @DisplayName("Aplica os pendentes e grava a tabela do empate triplo com o critério decisivo")
    void aplicaEClassifica() {
        empateTriplo();

        ResumoDaApuracao resumo = aplicador.processarChave("A");

        assertEquals(3, resumo.getAplicados());
        assertEquals(Arrays.asList("IFCE:Saldo de gols", "IFG:Confronto direto", "IFSP:Confronto direto"), ordem(historico.tabela("A")));
        LinhaDaTabela ifce = historico.tabela("A").get(0);
        assertEquals(3, ifce.getPontos());
        assertEquals(7, ifce.getPro());
        assertEquals(5, ifce.getContra());
        assertEquals(2, ifce.getSaldo());
        assertEquals(EstadoDoEvento.APLICADO, historico.estadoDe("e1"));
        assertEquals("worker-0", historico.workerDe("e1"));
        assertEquals(EstadoDaPartida.HOMOLOGADA, historico.partida(jogos.get(0)).getEstado());
    }

    @Test
    @DisplayName("RNF-03: mensagem repetida não aplica nada de novo e não muda a tabela")
    void mensagemRepetida() {
        empateTriplo();
        aplicador.processarChave("A");
        List<LinhaDaTabela> antes = historico.tabela("A");
        long versaoAntes = historico.versao("A");

        ResumoDaApuracao repetida = aplicador.processarChave("A");
        ResumoDaApuracao porOutroWorker = new AplicadorDeEventos(historico, CatalogoDeModalidades.padrao(), "worker-1").processarChave("A");

        assertTrue(repetida.nadaAFazer());
        assertTrue(porOutroWorker.nadaAFazer());
        assertEquals(antes, historico.tabela("A"));
        assertEquals(versaoAntes, historico.versao("A"));
        for (String evento : new String[] {"e1", "e2", "e3"}) {
            assertEquals(1, historico.aplicacoesDe(evento), evento + " aplicado uma vez só");
        }
    }

    @Test
    @DisplayName("RNF-03: mesmos eventos em outra ordem de chegada dão as mesmas posições")
    void mesmoEventoMesmaPosicao() {
        empateTriplo();
        aplicador.processarChave("A");

        HistoricoEmMemoria outro = new HistoricoEmMemoria(new RelogioManual());
        List<String> j = outro.criarChave("A", "Chave A", "FUTSAL", Arrays.asList("IFCE", "IFG", "IFSP"));
        AplicadorDeEventos outroAplicador = new AplicadorDeEventos(outro, CatalogoDeModalidades.padrao(), "worker-2");
        outro.gravarPendente(EventoDeResultado.gols("e3", "A", j.get(1), 3, 4));
        outroAplicador.processarChave("A");
        outro.gravarPendente(EventoDeResultado.gols("e1", "A", j.get(0), 4, 1));
        outro.gravarPendente(EventoDeResultado.gols("e2", "A", j.get(2), 3, 1));
        outroAplicador.processarChave("A");

        assertEquals(historico.tabela("A"), outro.tabela("A"));
    }

    @Test
    @DisplayName("RF-10, RNF-04: a retificação vale sobre o resultado anterior e a partida fica RETIFICADA")
    void retificacao() {
        empateTriplo();
        aplicador.processarChave("A");

        historico.gravarPendente(EventoDeResultado.gols("e4", "A", jogos.get(2), 1, 3)); // corrige IFG 3x1 IFSP para 1x3
        aplicador.processarChave("A");

        assertEquals(EstadoDaPartida.RETIFICADA, historico.partida(jogos.get(2)).getEstado());
        // IFSP passa a ter 6 pontos, IFCE 3 e IFG 0
        assertEquals(Arrays.asList("IFSP:Pontos", "IFCE:Pontos", "IFG:Pontos"), ordem(historico.tabela("A")));
        assertEquals(2, historico.versao("A"));
    }

    @Test
    @DisplayName("RNF-04: dois eventos da mesma partida na mesma leva são aplicados em ordem de sequência")
    void ordemDeSequencia() {
        historico.gravarPendente(EventoDeResultado.gols("e1", "A", jogos.get(0), 1, 0));
        historico.gravarPendente(EventoDeResultado.gols("e2", "A", jogos.get(0), 0, 2));

        ResumoDaApuracao resumo = aplicador.processarChave("A");

        assertEquals(2, resumo.getAplicados());
        assertEquals("e2", historico.partida(jogos.get(0)).getResultado().getEventoId(), "vale o último");
        assertEquals("IFG", historico.tabela("A").get(0).getEquipe());
    }

    @Test
    @DisplayName("Evento mais antigo que o resultado já aplicado é marcado SUPERADO e não muda a tabela")
    void eventoSuperado() {
        historico.gravarPendenteComSeq(EventoDeResultado.gols("novo", "A", jogos.get(0), 2, 0).comSeq(10));
        aplicador.processarChave("A");
        historico.gravarPendenteComSeq(EventoDeResultado.gols("antigo", "A", jogos.get(0), 0, 5).comSeq(5));

        ResumoDaApuracao resumo = aplicador.processarChave("A");

        assertEquals(1, resumo.getSuperados());
        assertEquals(EstadoDoEvento.SUPERADO, historico.estadoDe("antigo"));
        assertEquals("novo", historico.partida(jogos.get(0)).getResultado().getEventoId());
        assertEquals("IFCE", historico.tabela("A").get(0).getEquipe());
    }

    @Test
    @DisplayName("Evento inválido é REJEITADO com o motivo e não trava os outros")
    void eventoInvalido() {
        historico.gravarPendente(EventoDeResultado.gols("ruim", "A", "partida-que-nao-existe", 1, 0));
        historico.gravarPendente(EventoDeResultado.wo("wo-sem-lado", "A", jogos.get(1), null));
        historico.gravarPendente(EventoDeResultado.gols("bom", "A", jogos.get(0), 2, 0));

        ResumoDaApuracao resumo = aplicador.processarChave("A");

        assertEquals(1, resumo.getAplicados());
        assertEquals(2, resumo.getRejeitados());
        assertEquals(EstadoDoEvento.REJEITADO, historico.estadoDe("ruim"));
        assertTrue(historico.motivoDe("ruim").contains("não existe"));
        assertEquals(EstadoDoEvento.APLICADO, historico.estadoDe("bom"));
    }

    @Test
    @DisplayName("WO pelo fluxo de eventos: o placar segue o maior da chave")
    void woPeloFluxo() {
        historico.gravarPendente(EventoDeResultado.gols("e1", "A", jogos.get(0), 5, 1));       // IFCE 5 x 1 IFG
        historico.gravarPendente(EventoDeResultado.wo("e2", "A", jogos.get(2), Lado.VISITANTE)); // IFG vence IFSP por WO
        aplicador.processarChave("A");

        LinhaDaTabela ifg = historico.tabela("A").stream().filter(l -> l.getEquipe().equals("IFG")).findFirst().get();
        assertEquals(3, ifg.getPontos());
        assertEquals(6, ifg.getPro(), "1 gol no primeiro jogo mais 5 do WO");
        assertEquals(6, ifg.getContra(), "5 gols sofridos no primeiro jogo mais 1 do WO");
    }

    @Test
    @DisplayName("RNF-02: se o worker cai antes de confirmar, nada é gravado e o próximo worker aplica tudo")
    void quedaAntesDeConfirmar() {
        empateTriplo();
        historico.falharAoConfirmar("A");

        assertThrows(IllegalStateException.class, () -> aplicador.processarChave("A"));

        assertEquals(3, historico.contar(EstadoDoEvento.PENDENTE), "os eventos continuam pendentes");
        assertTrue(historico.tabela("A").isEmpty(), "nenhuma posição foi gravada");
        assertEquals(EstadoDaPartida.AGENDADA, historico.partida(jogos.get(0)).getEstado());

        AplicadorDeEventos novoDono = new AplicadorDeEventos(historico, CatalogoDeModalidades.padrao(), "worker-2");
        assertEquals(3, novoDono.processarChave("A").getAplicados());
        assertEquals("worker-2", historico.workerDe("e1"));
        assertEquals(1, historico.aplicacoesDe("e1"));
    }

    @Test
    @DisplayName("RNF-03: dois workers processando a mesma chave ao mesmo tempo não duplicam nada")
    void doisWorkersNaMesmaChave() throws Exception {
        HistoricoEmMemoria h = new HistoricoEmMemoria(new RelogioManual());
        List<String> equipes = Arrays.asList("T1", "T2", "T3", "T4", "T5");
        List<String> partidas = h.criarChave("Z", "Chave Z", "FUTSAL", equipes);
        AplicadorDeEventos w0 = new AplicadorDeEventos(h, CatalogoDeModalidades.padrao(), "worker-0");
        AplicadorDeEventos w1 = new AplicadorDeEventos(h, CatalogoDeModalidades.padrao(), "worker-1");
        ExecutorService threads = Executors.newFixedThreadPool(3);
        CountDownLatch fim = new CountDownLatch(3);
        int total = 200;

        threads.submit(() -> {
            for (int i = 0; i < total; i++) {
                h.gravarPendente(EventoDeResultado.gols("ev-" + i, "Z", partidas.get(i % partidas.size()), i % 4, (i * 7) % 5));
            }
            fim.countDown();
        });
        for (AplicadorDeEventos worker : Arrays.asList(w0, w1)) {
            threads.submit(() -> {
                for (int i = 0; i < 400; i++) {
                    worker.processarChave("Z");
                }
                fim.countDown();
            });
        }
        assertTrue(fim.await(30, TimeUnit.SECONDS));
        threads.shutdown();
        w0.processarChave("Z");

        assertEquals(total, h.contar(EstadoDoEvento.APLICADO));
        assertEquals(0, h.eventosAplicadosMaisDeUmaVez());
        // a tabela final é a dos últimos resultados de cada partida, calculada do zero
        HistoricoEmMemoria limpo = new HistoricoEmMemoria(new RelogioManual());
        List<String> p2 = limpo.criarChave("Z", "Chave Z", "FUTSAL", equipes);
        for (int i = total - partidas.size(); i < total; i++) {
            limpo.gravarPendente(EventoDeResultado.gols("ev-" + i, "Z", p2.get(i % p2.size()), i % 4, (i * 7) % 5));
        }
        new AplicadorDeEventos(limpo, CatalogoDeModalidades.padrao(), "worker-9").processarChave("Z");
        assertEquals(limpo.tabela("Z"), h.tabela("Z"));
    }
}
