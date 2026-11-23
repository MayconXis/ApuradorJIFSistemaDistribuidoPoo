package br.edu.ifg.apurador.cluster.apuracao;

/** O que aconteceu ao processar uma chave. Imutável. */
public final class ResumoDaApuracao {

    private final String chaveId;
    private final int aplicados;
    private final int rejeitados;
    private final int superados;

    public ResumoDaApuracao(String chaveId, int aplicados, int rejeitados, int superados) {
        this.chaveId = chaveId;
        this.aplicados = aplicados;
        this.rejeitados = rejeitados;
        this.superados = superados;
    }

    public String getChaveId() {
        return chaveId;
    }

    public int getAplicados() {
        return aplicados;
    }

    public int getRejeitados() {
        return rejeitados;
    }

    public int getSuperados() {
        return superados;
    }

    /** True quando a mensagem não tinha nada a fazer: era repetida ou chegou atrasada. */
    public boolean nadaAFazer() {
        return aplicados + rejeitados + superados == 0;
    }

    @Override
    public String toString() {
        return chaveId + ": " + aplicados + " aplicados, " + rejeitados + " rejeitados, " + superados + " superados";
    }
}
