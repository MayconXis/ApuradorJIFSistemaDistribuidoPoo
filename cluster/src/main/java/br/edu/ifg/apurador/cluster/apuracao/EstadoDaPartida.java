package br.edu.ifg.apurador.cluster.apuracao;

/**
 * Estados da partida no MVP. SUPOSIÇÃO SUP-07: o regulamento não define estados
 * de partida. Este modelo é da equipe.
 */
public enum EstadoDaPartida {
    /** Criada com a chave, ainda sem resultado. */
    AGENDADA,
    /** Tem resultado aplicado à classificação. */
    HOMOLOGADA,
    /** O resultado foi corrigido depois de homologado (hipótese de uso para protesto aceito). */
    RETIFICADA
}
