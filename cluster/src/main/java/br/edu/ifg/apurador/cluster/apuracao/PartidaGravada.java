package br.edu.ifg.apurador.cluster.apuracao;

/** Uma partida como está no histórico: quem joga, o estado e o último resultado aplicado. Imutável. */
public final class PartidaGravada {

    private final String id;
    private final String chaveId;
    private final String mandante;
    private final String visitante;
    private final EstadoDaPartida estado;
    private final EventoDeResultado resultado;

    public PartidaGravada(String id, String chaveId, String mandante, String visitante,
                          EstadoDaPartida estado, EventoDeResultado resultado) {
        this.id = id;
        this.chaveId = chaveId;
        this.mandante = mandante;
        this.visitante = visitante;
        this.estado = estado;
        this.resultado = resultado;
    }

    /** Partida recém-criada, sem resultado. */
    public static PartidaGravada agendada(String id, String chaveId, String mandante, String visitante) {
        return new PartidaGravada(id, chaveId, mandante, visitante, EstadoDaPartida.AGENDADA, null);
    }

    /** Cópia desta partida com o resultado e o estado novos. */
    public PartidaGravada comResultado(EventoDeResultado novo, EstadoDaPartida novoEstado) {
        return new PartidaGravada(id, chaveId, mandante, visitante, novoEstado, novo);
    }

    public String getId() {
        return id;
    }

    public String getChaveId() {
        return chaveId;
    }

    public String getMandante() {
        return mandante;
    }

    public String getVisitante() {
        return visitante;
    }

    public EstadoDaPartida getEstado() {
        return estado;
    }

    /** Último resultado aplicado. Null enquanto a partida está agendada. */
    public EventoDeResultado getResultado() {
        return resultado;
    }

    public boolean temResultado() {
        return resultado != null;
    }

    /** Sequência do último evento aplicado. Zero se não há resultado. */
    public long getSeqAplicada() {
        return resultado == null ? 0 : resultado.getSeq();
    }
}
