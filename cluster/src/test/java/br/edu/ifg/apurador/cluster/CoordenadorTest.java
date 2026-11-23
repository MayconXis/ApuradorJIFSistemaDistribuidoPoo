package br.edu.ifg.apurador.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifg.apurador.cluster.anel.AnelDeHashLimitado;
import br.edu.ifg.apurador.cluster.apoio.FilaEmMemoria;
import br.edu.ifg.apurador.cluster.apoio.HistoricoEmMemoria;
import br.edu.ifg.apurador.cluster.apoio.RelogioManual;
import br.edu.ifg.apurador.cluster.apuracao.EstadoDoEvento;
import br.edu.ifg.apurador.cluster.apuracao.EventoDeResultado;
import br.edu.ifg.apurador.cluster.despacho.Coordenador;
import br.edu.ifg.apurador.cluster.despacho.Recibo;
import br.edu.ifg.apurador.cluster.despacho.RegistradorDeResultados;
import br.edu.ifg.apurador.cluster.despacho.ResultadoInvalidoException;
import br.edu.ifg.apurador.cluster.despacho.Tarefa;
import br.edu.ifg.apurador.cluster.despacho.VisaoDoCluster;
import br.edu.ifg.apurador.cluster.despacho.VisaoDoWorker;
import br.edu.ifg.apurador.cluster.falhas.DetectorDeFalhas;
import br.edu.ifg.apurador.cluster.falhas.EstadoDoWorker;
import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TA-03. O Dispatcher: despacho, reatribuição e varredura (RNF-01, RNF-02, RNF-06, RNF-14, RF-02, RF-12). */
@DisplayName("TA-03 Dispatcher: despacho, reatribuição na queda e varredura de pendentes")
class CoordenadorTest {

    private RelogioManual relogio;
    private HistoricoEmMemoria historico;
    private FilaEmMemoria fila;
    private AnelDeHashLimitado anel;
    private Coordenador coordenador;
    private RegistradorDeResultados registrador;
    private List<String> partidasA;

    @BeforeEach
    void preparar() {
        relogio = new RelogioManual();
        historico = new HistoricoEmMemoria(relogio);
        fila = new FilaEmMemoria();
        anel = new AnelDeHashLimitado(64, 0.25);
        coordenador = new Coordenador(anel, new DetectorDeFalhas(relogio, 3000, 5000), fila, historico);
        registrador = new RegistradorDeResultados(historico, coordenador, CatalogoDeModalidades.padrao());
        partidasA = historico.criarChave("A", "Chave A", "FUTSAL", Arrays.asList("IFCE", "IFG", "IFSP"));
        for (String id : new String[] {"B", "C", "D", "E", "F"}) {
            historico.criarChave(id, "Chave " + id, "FUTSAL", Arrays.asList("X", "Y", "Z"));
        }
    }

    private void tresWorkersVivos() {
        for (String worker : new String[] {"worker-0", "worker-1", "worker-2"}) {
            coordenador.receberHeartbeat(worker);
        }
    }

    @Test
    @DisplayName("RF-02, RNF-04: todo resultado da mesma chave vai para a fila do mesmo worker")
    void afinidadePorChave() {
        tresWorkersVivos();
        Recibo primeiro = registrador.registrar(EventoDeResultado.gols("e1", "A", partidasA.get(0), 4, 1));
        Recibo segundo = registrador.registrar(EventoDeResultado.gols("e2", "A", partidasA.get(1), 3, 1));
        Recibo terceiro = registrador.registrar(EventoDeResultado.gols("e3", "A", partidasA.get(2), 4, 3));

        assertNotNull(primeiro.getWorker());
        assertEquals(primeiro.getWorker(), segundo.getWorker());
        assertEquals(primeiro.getWorker(), terceiro.getWorker());
        assertEquals(3, fila.tamanho(primeiro.getWorker()));
        assertTrue(primeiro.getSeq() < segundo.getSeq() && segundo.getSeq() < terceiro.getSeq());
    }

    @Test
    @DisplayName("RNF-02: o evento é gravado antes de publicar. Se a publicação falha, a varredura entrega")
    void gravaAntesDePublicar() {
        tresWorkersVivos();
        fila.falharNasProximas(1);

        Recibo recibo = registrador.registrar(EventoDeResultado.gols("e1", "A", partidasA.get(0), 4, 1));

        assertNull(recibo.getWorker(), "a publicação falhou");
        assertEquals(EstadoDoEvento.PENDENTE, historico.estadoDe("e1"), "mas o evento já está no histórico");
        assertEquals(0, coordenador.varrerPendentes(10_000), "ainda é recente demais para a varredura");

        relogio.avancar(10_000);
        assertEquals(1, coordenador.varrerPendentes(10_000));
        assertEquals(1, fila.tamanho(anel.donoAtual("A")));
    }

    @Test
    @DisplayName("RF-03: resultado inválido é recusado e nada é gravado nem publicado")
    void resultadoInvalido() {
        tresWorkersVivos();
        assertThrows(ResultadoInvalidoException.class, () -> registrador.registrar(EventoDeResultado.gols("e1", "A", "partida-que-nao-existe", 1, 0)));
        assertThrows(ResultadoInvalidoException.class, () -> registrador.registrar(EventoDeResultado.gols("e2", "A", partidasA.get(0), -1, 0)));
        assertThrows(ResultadoInvalidoException.class, () -> registrador.registrar(EventoDeResultado.wo("e3", "A", partidasA.get(0), null)));
        assertEquals(0, historico.totalDeEventos());
        assertTrue(fila.publicacoes().isEmpty());
    }

    @Test
    @DisplayName("RNF-03: o mesmo eventoId enviado duas vezes é gravado uma vez só")
    void envioRepetido() {
        tresWorkersVivos();
        Recibo primeiro = registrador.registrar(EventoDeResultado.gols("e1", "A", partidasA.get(0), 4, 1));
        Recibo repetido = registrador.registrar(EventoDeResultado.gols("e1", "A", partidasA.get(0), 4, 1));

        assertTrue(primeiro.isNovo());
        assertFalse(repetido.isNovo());
        assertEquals(primeiro.getSeq(), repetido.getSeq());
        assertEquals(1, historico.totalDeEventos());
    }

    @Test
    @DisplayName("Sem worker vivo, o resultado é aceito e espera. Quando um worker entra, ele recebe a tarefa")
    void semWorkerVivo() {
        Recibo recibo = registrador.registrar(EventoDeResultado.gols("e1", "A", partidasA.get(0), 4, 1));
        assertNull(recibo.getWorker());
        assertEquals(EstadoDoEvento.PENDENTE, historico.estadoDe("e1"));

        coordenador.receberHeartbeat("worker-0");

        assertEquals("worker-0", anel.donoAtual("A"));
        assertEquals(1, fila.tamanho("worker-0"), "a chave com pendente foi reenviada ao worker que entrou");
    }

    @Test
    @DisplayName("RNF-01, RNF-06: worker sem heartbeat é declarado morto, sai do anel e as chaves dele são reatribuídas")
    void quedaDeWorker() {
        tresWorkersVivos();
        for (String chave : historico.chavesConhecidas()) {
            coordenador.registrarChave(chave);
        }
        String donoDeA = anel.donoAtual("A");
        registrador.registrar(EventoDeResultado.gols("e1", "A", partidasA.get(0), 4, 1));
        List<String> chavesDoMorto = anel.chavesDe(donoDeA);

        // os outros dois continuam mandando heartbeat, o dono da chave A para
        for (int segundo = 1; segundo <= 6; segundo++) {
            relogio.avancar(1000);
            for (String worker : new String[] {"worker-0", "worker-1", "worker-2"}) {
                if (!worker.equals(donoDeA)) {
                    coordenador.receberHeartbeat(worker);
                }
            }
            coordenador.verificarFalhas();
        }

        assertFalse(anel.workers().contains(donoDeA), "o worker morto saiu do anel");
        String novoDono = anel.donoAtual("A");
        assertNotEquals(donoDeA, novoDono);
        for (String chave : chavesDoMorto) {
            assertNotEquals(donoDeA, anel.donoAtual(chave), chave + " tem novo dono");
        }
        assertTrue(fila.publicacoes().contains(novoDono + ":A:" + Tarefa.REATRIBUICAO), "a chave com pendente foi reenviada ao novo dono");
        long reatribuicoes = fila.publicacoes().stream().filter(p -> p.endsWith(Tarefa.REATRIBUICAO)).count();
        assertEquals(1, reatribuicoes, "só a chave que tinha evento pendente gerou tarefa");
    }

    @Test
    @DisplayName("RNF-06: worker SUSPEITO continua no anel. Só o MORTO sai")
    void suspeitoNaoSaiDoAnel() {
        tresWorkersVivos();
        relogio.avancar(4000);
        coordenador.receberHeartbeat("worker-0");
        coordenador.receberHeartbeat("worker-2");

        coordenador.verificarFalhas();

        assertTrue(anel.workers().contains("worker-1"));
        VisaoDoCluster visao = coordenador.visao();
        for (VisaoDoWorker worker : visao.getWorkers()) {
            assertEquals(worker.getId().equals("worker-1") ? EstadoDoWorker.SUSPEITO : EstadoDoWorker.VIVO, worker.getEstado());
        }
    }

    @Test
    @DisplayName("O worker que volta a mandar heartbeat entra de novo no anel")
    void workerVolta() {
        tresWorkersVivos();
        for (String chave : historico.chavesConhecidas()) {
            coordenador.registrarChave(chave);
        }
        relogio.avancar(6000);
        coordenador.receberHeartbeat("worker-0");
        coordenador.receberHeartbeat("worker-2");
        coordenador.verificarFalhas();
        assertFalse(anel.workers().contains("worker-1"));

        coordenador.receberHeartbeat("worker-1");

        assertTrue(anel.workers().contains("worker-1"));
        assertEquals(6, anel.atribuicoes().size(), "todas as chaves continuam com dono");
        assertTrue(coordenador.visao().getMudancas().get(0).startsWith("worker-1 voltou ao anel"));
    }

    @Test
    @DisplayName("RNF-14: Dispatcher reiniciado reconstrói o anel pelos heartbeats e pelo histórico")
    void dispatcherReiniciado() {
        tresWorkersVivos();
        registrador.registrar(EventoDeResultado.gols("e1", "A", partidasA.get(0), 4, 1));

        // o Dispatcher cai e volta: anel e detector novos, o histórico é o mesmo
        FilaEmMemoria filaNova = new FilaEmMemoria();
        AnelDeHashLimitado anelNovo = new AnelDeHashLimitado(64, 0.25);
        Coordenador reiniciado = new Coordenador(anelNovo, new DetectorDeFalhas(relogio, 3000, 5000), filaNova, historico);
        reiniciado.receberHeartbeat("worker-0");
        reiniciado.receberHeartbeat("worker-1");
        reiniciado.receberHeartbeat("worker-2");

        assertEquals(6, anelNovo.atribuicoes().size(), "as 6 chaves do histórico voltaram a ter dono");
        assertTrue(filaNova.publicacoes().stream().anyMatch(p -> p.contains(":A:")), "a chave com evento pendente foi reenviada");
        assertEquals(EstadoDoEvento.PENDENTE, historico.estadoDe("e1"), "o evento aceito antes da queda não se perdeu");
    }

    @Test
    @DisplayName("RF-12: o painel mostra estado, heartbeat, chaves e teto")
    void painel() {
        tresWorkersVivos();
        for (String chave : historico.chavesConhecidas()) {
            coordenador.registrarChave(chave);
        }
        relogio.avancar(700);

        VisaoDoCluster visao = coordenador.visao();

        assertEquals(3, visao.getWorkers().size());
        assertEquals(6, visao.getTotalDeChaves());
        assertEquals(3, visao.getTeto(), "teto = arredonda para cima (1,25 x 6 / 3)");
        int soma = 0;
        for (VisaoDoWorker worker : visao.getWorkers()) {
            assertEquals(700, worker.getSilencioMs());
            assertTrue(worker.getCarga() <= visao.getTeto());
            soma += worker.getCarga();
        }
        assertEquals(6, soma);
    }
}
