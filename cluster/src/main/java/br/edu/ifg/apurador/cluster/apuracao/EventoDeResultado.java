package br.edu.ifg.apurador.cluster.apuracao;

import br.edu.ifg.apurador.core.sumula.Disciplina;
import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.core.sumula.Placar;

/**
 * Um resultado enviado ao sistema. Imutável.
 *
 * O eventoId é gerado por quem envia e é único: é ele que permite reconhecer
 * o mesmo resultado quando ele chega duas vezes. A seq é dada pelo banco na
 * gravação e define a ordem dos eventos.
 */
public final class EventoDeResultado {

    private final String eventoId;
    private final long seq;
    private final String chaveId;
    private final String partidaId;
    private final int placarMandante;
    private final int placarVisitante;
    private final int[][] parciais;
    private final boolean wo;
    private final Lado ausente;
    private final int[] amarelos;
    private final int[] vermelhos;
    private final int[] sancoes;

    public EventoDeResultado(String eventoId, long seq, String chaveId, String partidaId,
                             int placarMandante, int placarVisitante, int[][] parciais, boolean wo, Lado ausente,
                             int[] amarelos, int[] vermelhos, int[] sancoes) {
        if (eventoId == null || eventoId.trim().isEmpty()) {
            throw new IllegalArgumentException("O evento precisa de um identificador.");
        }
        this.eventoId = eventoId.trim();
        this.seq = seq;
        this.chaveId = chaveId;
        this.partidaId = partidaId;
        this.placarMandante = placarMandante;
        this.placarVisitante = placarVisitante;
        this.parciais = copiar(parciais);
        this.wo = wo;
        this.ausente = ausente;
        this.amarelos = par(amarelos);
        this.vermelhos = par(vermelhos);
        this.sancoes = par(sancoes);
    }

    /** Resultado em gols (futsal e handebol). */
    public static EventoDeResultado gols(String eventoId, String chaveId, String partidaId, int mandante, int visitante) {
        return new EventoDeResultado(eventoId, 0, chaveId, partidaId, mandante, visitante, null, false, null, null, null, null);
    }

    /** Resultado em sets, com as parciais (voleibol). */
    public static EventoDeResultado sets(String eventoId, String chaveId, String partidaId, int[][] parciais) {
        return new EventoDeResultado(eventoId, 0, chaveId, partidaId, 0, 0, parciais, false, null, null, null, null);
    }

    /** Vitória por WO: informa só quem não compareceu. */
    public static EventoDeResultado wo(String eventoId, String chaveId, String partidaId, Lado ausente) {
        return new EventoDeResultado(eventoId, 0, chaveId, partidaId, 0, 0, null, true, ausente, null, null, null);
    }

    public EventoDeResultado comCartoes(int amarelosMandante, int amarelosVisitante, int vermelhosMandante, int vermelhosVisitante) {
        return new EventoDeResultado(eventoId, seq, chaveId, partidaId, placarMandante, placarVisitante, parciais, wo, ausente,
                new int[] {amarelosMandante, amarelosVisitante}, new int[] {vermelhosMandante, vermelhosVisitante}, sancoes);
    }

    public EventoDeResultado comSancoes(int sancoesMandante, int sancoesVisitante) {
        return new EventoDeResultado(eventoId, seq, chaveId, partidaId, placarMandante, placarVisitante, parciais, wo, ausente,
                amarelos, vermelhos, new int[] {sancoesMandante, sancoesVisitante});
    }

    public EventoDeResultado comSeq(long novaSeq) {
        return new EventoDeResultado(eventoId, novaSeq, chaveId, partidaId, placarMandante, placarVisitante, parciais, wo, ausente,
                amarelos, vermelhos, sancoes);
    }

    /** Placar do core. Com parciais, é placar em sets. Sem parciais, em gols. Null no WO. */
    public Placar placar() {
        if (wo) {
            return null;
        }
        return parciais.length > 0 ? Placar.emSets(parciais) : new Placar(placarMandante, placarVisitante);
    }

    public Disciplina disciplina() {
        return new Disciplina(amarelos[0], amarelos[1], vermelhos[0], vermelhos[1], sancoes[0], sancoes[1]);
    }

    public String getEventoId() {
        return eventoId;
    }

    public long getSeq() {
        return seq;
    }

    public String getChaveId() {
        return chaveId;
    }

    public String getPartidaId() {
        return partidaId;
    }

    public int getPlacarMandante() {
        return placarMandante;
    }

    public int getPlacarVisitante() {
        return placarVisitante;
    }

    public int[][] getParciais() {
        return copiar(parciais);
    }

    public boolean isWo() {
        return wo;
    }

    public Lado getAusente() {
        return ausente;
    }

    public int amarelos(Lado lado) {
        return amarelos[lado.ordinal()];
    }

    public int vermelhos(Lado lado) {
        return vermelhos[lado.ordinal()];
    }

    public int sancoes(Lado lado) {
        return sancoes[lado.ordinal()];
    }

    private static int[] par(int[] valores) {
        return valores == null ? new int[] {0, 0} : new int[] {valores[0], valores[1]};
    }

    private static int[][] copiar(int[][] origem) {
        if (origem == null) {
            return new int[0][2];
        }
        int[][] copia = new int[origem.length][];
        for (int i = 0; i < origem.length; i++) {
            copia[i] = origem[i] == null ? new int[0] : origem[i].clone();
        }
        return copia;
    }

    @Override
    public String toString() {
        return "Evento{" + eventoId + ", seq " + seq + ", partida " + partidaId + "}";
    }
}
