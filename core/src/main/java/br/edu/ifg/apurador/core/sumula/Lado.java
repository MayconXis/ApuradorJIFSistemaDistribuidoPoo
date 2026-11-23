package br.edu.ifg.apurador.core.sumula;

/** Lado de uma equipe na partida. */
public enum Lado {
    MANDANTE,
    VISITANTE;

    public Lado oposto() {
        return this == MANDANTE ? VISITANTE : MANDANTE;
    }
}
