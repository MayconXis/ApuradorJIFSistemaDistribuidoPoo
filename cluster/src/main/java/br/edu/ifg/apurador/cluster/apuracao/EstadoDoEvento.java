package br.edu.ifg.apurador.cluster.apuracao;

/** Situação de um evento de resultado no histórico. */
public enum EstadoDoEvento {
    /** Gravado pelo Gateway, ainda não aplicado à tabela. */
    PENDENTE,
    /** Aplicado à partida e à classificação, uma única vez. */
    APLICADO,
    /** Recusado na aplicação, com o motivo gravado. Não muda a tabela. */
    REJEITADO,
    /** Chegou depois de um resultado mais novo da mesma partida. Não muda a tabela. */
    SUPERADO
}
