package br.edu.ifg.apurador.cluster.apoio;

import br.edu.ifg.apurador.cluster.apuracao.AcaoNaChave;
import br.edu.ifg.apurador.cluster.apuracao.DadosDaChave;
import br.edu.ifg.apurador.cluster.apuracao.EstadoDaPartida;
import br.edu.ifg.apurador.cluster.apuracao.EstadoDoEvento;
import br.edu.ifg.apurador.cluster.apuracao.EventoDeResultado;
import br.edu.ifg.apurador.cluster.apuracao.HistoricoDaChave;
import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import br.edu.ifg.apurador.cluster.apuracao.PartidaGravada;
import br.edu.ifg.apurador.cluster.apuracao.TransacaoDaChave;
import br.edu.ifg.apurador.cluster.despacho.ConsultaDePendencias;
import br.edu.ifg.apurador.cluster.despacho.Recibo;
import br.edu.ifg.apurador.cluster.despacho.RegistroDeEventos;
import br.edu.ifg.apurador.cluster.falhas.Relogio;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Histórico de mentira para os testes, no lugar do PostgreSQL. Imita as duas
 * garantias do banco que o algoritmo usa: a trava por chave e a transação que
 * confirma tudo junto ou desfaz tudo.
 */
public final class HistoricoEmMemoria implements HistoricoDaChave, RegistroDeEventos, ConsultaDePendencias {

    /** Linha do evento no histórico. */
    private static final class Registro {
        EventoDeResultado evento;
        EstadoDoEvento estado = EstadoDoEvento.PENDENTE;
        String worker;
        String motivo;
        long recebidoEm;
        int aplicacoes;
    }

    private final Relogio relogio;
    private final Map<String, DadosDaChave> chaves = new LinkedHashMap<>();
    private final Map<String, PartidaGravada> partidas = new LinkedHashMap<>();
    private final Map<String, Registro> eventos = new LinkedHashMap<>();
    private final Map<String, List<LinhaDaTabela>> tabelas = new HashMap<>();
    private final Map<String, Long> versoes = new HashMap<>();
    private final Map<String, ReentrantLock> travas = new HashMap<>();
    private long sequencia;
    private String falharAoConfirmar;

    public HistoricoEmMemoria(Relogio relogio) {
        this.relogio = relogio;
    }

    /** Cadastra a chave e gera as partidas em rodízio simples. Devolve os identificadores das partidas. */
    public synchronized List<String> criarChave(String id, String nome, String modalidade, List<String> equipes) {
        chaves.put(id, new DadosDaChave(id, nome, modalidade, equipes));
        List<String> ids = new ArrayList<>();
        int numero = 1;
        for (int i = 0; i < equipes.size(); i++) {
            for (int j = i + 1; j < equipes.size(); j++) {
                String partidaId = id + "-J" + numero++;
                partidas.put(partidaId, PartidaGravada.agendada(partidaId, id, equipes.get(i), equipes.get(j)));
                ids.add(partidaId);
            }
        }
        return ids;
    }

    // ---------- porta do Gateway ----------

    @Override
    public synchronized PartidaGravada partida(String partidaId) {
        return partidas.get(partidaId);
    }

    @Override
    public synchronized DadosDaChave chave(String chaveId) {
        return chaves.get(chaveId);
    }

    @Override
    public synchronized Recibo gravarPendente(EventoDeResultado evento) {
        Registro existente = eventos.get(evento.getEventoId());
        if (existente != null) {
            return new Recibo(evento.getEventoId(), existente.evento.getSeq(), false, null);
        }
        Registro novo = new Registro();
        novo.evento = evento.comSeq(++sequencia);
        novo.recebidoEm = relogio.agora();
        eventos.put(evento.getEventoId(), novo);
        return new Recibo(evento.getEventoId(), novo.evento.getSeq(), true, null);
    }

    /** Grava um evento com a sequência informada, para simular gravações que chegam fora de ordem. */
    public synchronized void gravarPendenteComSeq(EventoDeResultado evento) {
        Registro novo = new Registro();
        novo.evento = evento;
        novo.recebidoEm = relogio.agora();
        eventos.put(evento.getEventoId(), novo);
        sequencia = Math.max(sequencia, evento.getSeq());
    }

    // ---------- porta do Dispatcher ----------

    @Override
    public synchronized List<String> chavesConhecidas() {
        return new ArrayList<>(chaves.keySet());
    }

    @Override
    public synchronized List<String> chavesComPendentes(long idadeMinimaMs) {
        List<String> resultado = new ArrayList<>();
        long agora = relogio.agora();
        for (Registro registro : eventos.values()) {
            String chave = registro.evento.getChaveId();
            if (registro.estado == EstadoDoEvento.PENDENTE && agora - registro.recebidoEm >= idadeMinimaMs
                    && !resultado.contains(chave)) {
                resultado.add(chave);
            }
        }
        return resultado;
    }

    // ---------- porta do worker ----------

    @Override
    public <T> T comTrava(String chaveId, AcaoNaChave<T> acao) {
        ReentrantLock trava;
        synchronized (this) {
            trava = travas.computeIfAbsent(chaveId, c -> new ReentrantLock());
        }
        trava.lock();
        try {
            Transacao transacao = new Transacao(chaveId);
            T resultado = acao.executar(transacao);
            synchronized (this) {
                if (chaveId.equals(falharAoConfirmar)) {
                    falharAoConfirmar = null;
                    throw new IllegalStateException("worker caiu antes de confirmar a transação (falha simulada)");
                }
                transacao.confirmar();
            }
            return resultado;
        } finally {
            trava.unlock();
        }
    }

    /** A próxima transação desta chave vai falhar na hora de confirmar: nada do que ela fez é gravado. */
    public synchronized void falharAoConfirmar(String chaveId) {
        falharAoConfirmar = chaveId;
    }

    /** Transação: guarda o que foi pedido e só grava no histórico em confirmar(). */
    private final class Transacao implements TransacaoDaChave {

        private final String chaveId;
        private final List<Runnable> gravacoes = new ArrayList<>();

        Transacao(String chaveId) {
            this.chaveId = chaveId;
        }

        void confirmar() {
            for (Runnable gravacao : gravacoes) {
                gravacao.run();
            }
        }

        @Override
        public DadosDaChave chave() {
            synchronized (HistoricoEmMemoria.this) {
                return chaves.get(chaveId);
            }
        }

        @Override
        public List<EventoDeResultado> pendentes() {
            synchronized (HistoricoEmMemoria.this) {
                List<EventoDeResultado> lista = new ArrayList<>();
                for (Registro registro : eventos.values()) {
                    if (registro.estado == EstadoDoEvento.PENDENTE && chaveId.equals(registro.evento.getChaveId())) {
                        lista.add(registro.evento);
                    }
                }
                lista.sort((a, b) -> Long.compare(a.getSeq(), b.getSeq()));
                return lista;
            }
        }

        @Override
        public List<PartidaGravada> partidas() {
            return partidasDa(chaveId);
        }

        @Override
        public void aplicar(EventoDeResultado evento, EstadoDaPartida novoEstado, String worker) {
            gravacoes.add(() -> {
                PartidaGravada atual = partidas.get(evento.getPartidaId());
                partidas.put(atual.getId(), atual.comResultado(evento, novoEstado));
                Registro registro = eventos.get(evento.getEventoId());
                registro.estado = EstadoDoEvento.APLICADO;
                registro.worker = worker;
                registro.aplicacoes++;
            });
        }

        @Override
        public void encerrar(EventoDeResultado evento, EstadoDoEvento estado, String motivo, String worker) {
            gravacoes.add(() -> {
                Registro registro = eventos.get(evento.getEventoId());
                registro.estado = estado;
                registro.motivo = motivo;
                registro.worker = worker;
            });
        }

        @Override
        public void gravarClassificacao(List<LinhaDaTabela> linhas) {
            gravacoes.add(() -> {
                tabelas.put(chaveId, new ArrayList<>(linhas));
                versoes.merge(chaveId, 1L, Long::sum);
            });
        }
    }

    // ---------- consultas para as verificações dos testes ----------

    public synchronized List<PartidaGravada> partidasDa(String chaveId) {
        List<PartidaGravada> lista = new ArrayList<>();
        for (PartidaGravada partida : partidas.values()) {
            if (partida.getChaveId().equals(chaveId)) {
                lista.add(partida);
            }
        }
        return lista;
    }

    public synchronized List<LinhaDaTabela> tabela(String chaveId) {
        List<LinhaDaTabela> tabela = tabelas.get(chaveId);
        return tabela == null ? new ArrayList<>() : new ArrayList<>(tabela);
    }

    public synchronized long versao(String chaveId) {
        return versoes.getOrDefault(chaveId, 0L);
    }

    public synchronized EstadoDoEvento estadoDe(String eventoId) {
        return eventos.get(eventoId).estado;
    }

    public synchronized String motivoDe(String eventoId) {
        return eventos.get(eventoId).motivo;
    }

    public synchronized String workerDe(String eventoId) {
        return eventos.get(eventoId).worker;
    }

    /** Quantas vezes o evento foi aplicado à tabela. O certo é sempre 1. */
    public synchronized int aplicacoesDe(String eventoId) {
        return eventos.get(eventoId).aplicacoes;
    }

    public synchronized int contar(EstadoDoEvento estado) {
        int total = 0;
        for (Registro registro : eventos.values()) {
            if (registro.estado == estado) {
                total++;
            }
        }
        return total;
    }

    public synchronized int totalDeEventos() {
        return eventos.size();
    }

    /** Eventos aplicados mais de uma vez. O certo é zero. */
    public synchronized int eventosAplicadosMaisDeUmaVez() {
        int total = 0;
        for (Registro registro : eventos.values()) {
            if (registro.aplicacoes > 1) {
                total++;
            }
        }
        return total;
    }

    public synchronized Map<String, DadosDaChave> chaves() {
        return new LinkedHashMap<>(chaves);
    }
}
