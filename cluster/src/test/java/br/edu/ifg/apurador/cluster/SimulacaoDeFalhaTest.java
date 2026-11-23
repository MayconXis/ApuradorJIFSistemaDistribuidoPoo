package br.edu.ifg.apurador.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifg.apurador.cluster.anel.AnelDeHashLimitado;
import br.edu.ifg.apurador.cluster.apoio.FilaEmMemoria;
import br.edu.ifg.apurador.cluster.apoio.HistoricoEmMemoria;
import br.edu.ifg.apurador.cluster.apoio.RelogioManual;
import br.edu.ifg.apurador.cluster.apuracao.AplicadorDeEventos;
import br.edu.ifg.apurador.cluster.apuracao.CalculoDaTabela;
import br.edu.ifg.apurador.cluster.apuracao.DadosDaChave;
import br.edu.ifg.apurador.cluster.apuracao.EstadoDaPartida;
import br.edu.ifg.apurador.cluster.apuracao.EstadoDoEvento;
import br.edu.ifg.apurador.cluster.apuracao.EventoDeResultado;
import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import br.edu.ifg.apurador.cluster.apuracao.PartidaGravada;
import br.edu.ifg.apurador.cluster.despacho.Coordenador;
import br.edu.ifg.apurador.cluster.despacho.RegistradorDeResultados;
import br.edu.ifg.apurador.cluster.despacho.Tarefa;
import br.edu.ifg.apurador.cluster.falhas.DetectorDeFalhas;
import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import br.edu.ifg.apurador.core.sumula.Lado;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TI-01. Integração simulada, sem infraestrutura: Gateway, Dispatcher, três
 * workers, fila e histórico em memória, com relógio manual.
 *
 * Cenário: rajada de resultados em 12 chaves, com envios repetidos e
 * retificações. No meio da rajada o worker-1 cai duas vezes de uma vez só:
 * uma tarefa foi gravada e não confirmada, a outra caiu antes de gravar.
 * O detector o declara morto, as chaves são reatribuídas e, no fim, ele volta
 * e recebe de novo as mensagens antigas.
 *
 * O que o teste prova é o comportamento dos algoritmos do grupo. Ele não mede
 * desempenho: o tempo é simulado. Os números de latência e de recuperação no
 * cluster real foram medidos com k6 e Chaos Mesh (relatório técnico, seção 8.3).
 */
@DisplayName("TI-01 Integração simulada: rajada com queda de worker (RNF-01, RNF-02, RNF-03, RNF-04, RNF-05)")
class SimulacaoDeFalhaTest {

    private static final String[] WORKERS = {"worker-0", "worker-1", "worker-2"};
    private static final int PASSO_MS = 100;
    private static final int INTERVALO_HEARTBEAT_MS = 1000;
    private static final int LIMITE_SUSPEITA_MS = 3000;
    private static final int TEMPO_LIMITE_MS = 5000;

    private final Random sorteio = new Random(20261116);
    private final RelogioManual relogio = new RelogioManual();
    private final HistoricoEmMemoria historico = new HistoricoEmMemoria(relogio);
    private final FilaEmMemoria fila = new FilaEmMemoria();
    private final AnelDeHashLimitado anel = new AnelDeHashLimitado(64, 0.25);
    private final Coordenador coordenador = new Coordenador(anel, new DetectorDeFalhas(relogio, LIMITE_SUSPEITA_MS, TEMPO_LIMITE_MS), fila, historico);
    private final CatalogoDeModalidades catalogo = CatalogoDeModalidades.padrao();
    private final RegistradorDeResultados registrador = new RegistradorDeResultados(historico, coordenador, catalogo);
    private final Map<String, AplicadorDeEventos> aplicadores = new LinkedHashMap<>();

    /** Resultado final esperado de cada partida: o último evento enviado para ela. */
    private final Map<String, EventoDeResultado> esperado = new LinkedHashMap<>();
    private final List<EventoDeResultado> envios = new ArrayList<>();
    private int mensagensSemNadaAFazer;
    private int maiorCargaVista;

    @Test
    @DisplayName("Zero eventos perdidos, zero duplicatas e tabela igual à calculada direto pelo core")
    void rajadaComQuedaDeWorker() {
        for (String worker : WORKERS) {
            aplicadores.put(worker, new AplicadorDeEventos(historico, catalogo, worker));
        }
        montarChavesEEnvios();
        int totalDeEnvios = envios.size();

        boolean worker1Vivo = true;
        boolean jaCaiu = false;
        long instanteDaQueda = -1;
        long instanteDaReatribuicao = -1;
        int tarefasPresasNoMorto = 0;
        int proximoEnvio = 0;
        List<String> chavesDoWorker1NaQueda = new ArrayList<>();

        for (long t = 0; t <= 60_000; t += PASSO_MS) {
            // 1. heartbeats dos workers vivos
            if (t % INTERVALO_HEARTBEAT_MS == 0) {
                for (String worker : WORKERS) {
                    if (worker1Vivo || !worker.equals("worker-1")) {
                        coordenador.receberHeartbeat(worker);
                    }
                }
            }
            // 2. Gateway recebe a rajada: 6 envios por passo
            for (int i = 0; i < 6 && proximoEnvio < totalDeEnvios; i++) {
                registrador.registrar(envios.get(proximoEnvio++));
            }
            // 3. o worker-1 cai no meio da rajada, com duas tarefas em andamento
            if (!jaCaiu && proximoEnvio >= totalDeEnvios / 2) {
                jaCaiu = true;
                chavesDoWorker1NaQueda = anel.chavesDe("worker-1");
                Tarefa gravadaSemAck = fila.receber("worker-1");
                if (gravadaSemAck != null) {
                    aplicadores.get("worker-1").processarChave(gravadaSemAck.getChaveId()); // gravou, mas caiu antes do ack
                }
                Tarefa caiuAntesDeGravar = fila.receber("worker-1");
                if (caiuAntesDeGravar != null) {
                    historico.falharAoConfirmar(caiuAntesDeGravar.getChaveId());
                    try {
                        aplicadores.get("worker-1").processarChave(caiuAntesDeGravar.getChaveId());
                    } catch (IllegalStateException quedaSimulada) {
                        // esperado: a transação foi desfeita
                    }
                }
                fila.devolverNaoConfirmadas("worker-1");
                tarefasPresasNoMorto = fila.tamanho("worker-1");
                worker1Vivo = false;
                instanteDaQueda = t;
            }
            // 4. detector e varredura, uma vez por segundo
            if (t % 1000 == 0) {
                coordenador.verificarFalhas();
                coordenador.varrerPendentes(10_000);
                if (!worker1Vivo && instanteDaReatribuicao < 0 && !anel.workers().contains("worker-1")) {
                    instanteDaReatribuicao = t;
                }
            }
            // 5. cada worker vivo processa até 4 tarefas por passo
            for (String worker : WORKERS) {
                if (!worker1Vivo && worker.equals("worker-1")) {
                    continue;
                }
                for (int i = 0; i < 4; i++) {
                    Tarefa tarefa = fila.receber(worker);
                    if (tarefa == null) {
                        break;
                    }
                    if (aplicadores.get(worker).processarChave(tarefa.getChaveId()).nadaAFazer()) {
                        mensagensSemNadaAFazer++;
                    }
                    fila.confirmar(worker, tarefa);
                }
            }
            for (String worker : anel.workers()) {
                maiorCargaVista = Math.max(maiorCargaVista, anel.cargaDe(worker));
                assertTrue(anel.cargaDe(worker) <= anel.teto(), "carga acima do teto em t=" + t);
            }
            // 6. depois de tudo processado, o k3s "recria" o worker-1
            if (!worker1Vivo && instanteDaReatribuicao > 0 && t >= instanteDaReatribuicao + 10_000) {
                worker1Vivo = true;
            }
            relogio.avancar(PASSO_MS);
        }

        // ---------- verificações ----------
        assertTrue(instanteDaQueda >= 0, "a queda aconteceu durante a rajada");
        assertTrue(tarefasPresasNoMorto >= 2, "havia tarefas na fila do worker morto");
        assertFalse(chavesDoWorker1NaQueda.isEmpty(), "o worker-1 tinha chaves quando caiu");
        long recuperacao = instanteDaReatribuicao - instanteDaQueda;
        assertTrue(recuperacao > TEMPO_LIMITE_MS && recuperacao <= TEMPO_LIMITE_MS + INTERVALO_HEARTBEAT_MS + 1000,
                "a reatribuição acontece logo depois do tempo limite: " + recuperacao + " ms");

        int distintos = historico.totalDeEventos();
        int perdidos = historico.contar(EstadoDoEvento.PENDENTE);
        int duplicados = historico.eventosAplicadosMaisDeUmaVez();
        assertEquals(0, perdidos, "eventos perdidos");
        assertEquals(0, duplicados, "eventos aplicados mais de uma vez");
        assertEquals(distintos, historico.contar(EstadoDoEvento.APLICADO), "todo evento aceito foi aplicado");
        assertEquals(0, historico.contar(EstadoDoEvento.REJEITADO));
        assertTrue(mensagensSemNadaAFazer > 0, "houve mensagem repetida, e ela foi ignorada");
        assertTrue(anel.workers().contains("worker-1"), "o worker-1 voltou ao anel");
        for (String worker : WORKERS) {
            assertEquals(0, fila.tamanho(worker), "fila vazia no fim: " + worker);
        }

        // a tabela gravada de cada chave é igual à calculada direto, do zero, com os resultados finais
        CalculoDaTabela calculo = new CalculoDaTabela();
        for (DadosDaChave chave : historico.chaves().values()) {
            List<PartidaGravada> finais = new ArrayList<>();
            for (PartidaGravada partida : historico.partidasDa(chave.getId())) {
                EventoDeResultado ultimo = esperado.get(partida.getId());
                finais.add(ultimo == null ? partida : PartidaGravada.agendada(partida.getId(), chave.getId(), partida.getMandante(), partida.getVisitante())
                        .comResultado(ultimo, EstadoDaPartida.HOMOLOGADA));
            }
            List<LinhaDaTabela> direto = calculo.calcular(chave, finais, catalogo.buscar(chave.getModalidade()));
            assertEquals(direto, historico.tabela(chave.getId()), "tabela da chave " + chave.getId());
        }

        System.out.println("[TI-01] envios ao Gateway: " + totalDeEnvios + " (eventos distintos: " + distintos + ", envios repetidos: " + (totalDeEnvios - distintos) + ")");
        System.out.println("[TI-01] aplicados: " + historico.contar(EstadoDoEvento.APLICADO) + " | perdidos: " + perdidos + " | aplicados mais de uma vez: " + duplicados);
        System.out.println("[TI-01] mensagens repetidas ou atrasadas ignoradas pelos workers: " + mensagensSemNadaAFazer);
        System.out.println("[TI-01] chaves do worker-1 na queda: " + chavesDoWorker1NaQueda.size() + " | tarefas presas na fila dele: " + tarefasPresasNoMorto);
        System.out.println("[TI-01] da queda à reatribuição: " + recuperacao + " ms de tempo SIMULADO (tempo limite de " + TEMPO_LIMITE_MS + " ms mais o intervalo de verificação)");
        System.out.println("[TI-01] maior carga vista em um worker: " + maiorCargaVista + " chaves, com 2 workers vivos (teto de 8) | teto no fim, com 3 workers: " + anel.teto());
    }

    // ---------- geração do cenário ----------

    private void montarChavesEEnvios() {
        String[] modalidades = {"FUTSAL", "VOLEIBOL", "HANDEBOL"};
        int numero = 0;
        for (int c = 0; c < 12; c++) {
            String modalidade = modalidades[c % 3];
            String id = "chave-" + (char) ('A' + c);
            int qtdEquipes = 3 + (c % 3);
            List<String> equipes = new ArrayList<>();
            for (int e = 0; e < qtdEquipes; e++) {
                equipes.add("Equipe " + (char) ('A' + c) + (e + 1));
            }
            for (String partida : historico.criarChave(id, "Chave " + (char) ('A' + c), modalidade, equipes)) {
                EventoDeResultado evento = sortear("ev-" + (++numero), id, partida, modalidade);
                envios.add(evento);
                esperado.put(partida, evento);
                if (sorteio.nextInt(10) == 0) { // 10% das partidas têm retificação
                    EventoDeResultado correcao = sortear("ev-" + (++numero), id, partida, modalidade);
                    envios.add(correcao);
                    esperado.put(partida, correcao);
                }
            }
        }
        // embaralha os envios de chaves diferentes, mantendo a ordem dentro de cada partida
        List<EventoDeResultado> embaralhados = new ArrayList<>();
        Map<String, List<EventoDeResultado>> porPartida = new LinkedHashMap<>();
        for (EventoDeResultado evento : envios) {
            porPartida.computeIfAbsent(evento.getPartidaId(), p -> new ArrayList<>()).add(evento);
        }
        List<String> partidas = new ArrayList<>(porPartida.keySet());
        while (!partidas.isEmpty()) {
            String partida = partidas.get(sorteio.nextInt(partidas.size()));
            List<EventoDeResultado> filaDaPartida = porPartida.get(partida);
            EventoDeResultado evento = filaDaPartida.remove(0);
            embaralhados.add(evento);
            if (sorteio.nextInt(8) == 0) {
                embaralhados.add(evento); // o operador clicou duas vezes: mesmo eventoId
            }
            if (filaDaPartida.isEmpty()) {
                partidas.remove(partida);
            }
        }
        envios.clear();
        envios.addAll(embaralhados);
    }

    private EventoDeResultado sortear(String eventoId, String chave, String partida, String modalidade) {
        if (sorteio.nextInt(12) == 0) {
            return EventoDeResultado.wo(eventoId, chave, partida, sorteio.nextBoolean() ? Lado.MANDANTE : Lado.VISITANTE);
        }
        switch (modalidade) {
            case "VOLEIBOL":
                boolean mandanteVence = sorteio.nextBoolean();
                boolean tresSets = sorteio.nextBoolean();
                List<int[]> parciais = new ArrayList<>();
                parciais.add(set(mandanteVence, 25));
                if (tresSets) {
                    parciais.add(set(!mandanteVence, 25));
                    parciais.add(set(mandanteVence, 15));
                } else {
                    parciais.add(set(mandanteVence, 25));
                }
                return EventoDeResultado.sets(eventoId, chave, partida, parciais.toArray(new int[0][]));
            case "HANDEBOL":
                return EventoDeResultado.gols(eventoId, chave, partida, 15 + sorteio.nextInt(20), 15 + sorteio.nextInt(20))
                        .comSancoes(sorteio.nextInt(12), sorteio.nextInt(12));
            default:
                return EventoDeResultado.gols(eventoId, chave, partida, sorteio.nextInt(6), sorteio.nextInt(6))
                        .comCartoes(sorteio.nextInt(4), sorteio.nextInt(4), sorteio.nextInt(2), sorteio.nextInt(2));
        }
    }

    private int[] set(boolean mandanteVence, int alvo) {
        int perdedor = sorteio.nextInt(alvo - 1);
        return mandanteVence ? new int[] {alvo, perdedor} : new int[] {perdedor, alvo};
    }

    @Test
    @DisplayName("O cenário usa as três modalidades e chaves de 3, 4 e 5 equipes")
    void cenarioCobreAsTresModalidades() {
        montarChavesEEnvios();
        assertEquals(12, historico.chaves().size());
        assertTrue(envios.size() > 70, "rajada com mais de 70 envios: " + envios.size());
        assertEquals(Arrays.asList("FUTSAL", "VOLEIBOL", "HANDEBOL"),
                Arrays.asList(historico.chave("chave-A").getModalidade(), historico.chave("chave-B").getModalidade(), historico.chave("chave-C").getModalidade()));
    }
}
