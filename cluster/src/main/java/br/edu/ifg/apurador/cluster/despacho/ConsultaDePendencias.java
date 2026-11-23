package br.edu.ifg.apurador.cluster.despacho;

import java.util.List;

/**
 * O que o Dispatcher precisa saber do histórico no PostgreSQL para reatribuir
 * chaves e para a varredura de pendentes.
 */
public interface ConsultaDePendencias {

    /** Todas as chaves cadastradas, na ordem de criação. */
    List<String> chavesConhecidas();

    /** Chaves que têm pelo menos um evento pendente há mais tempo que o informado. Zero devolve todas. */
    List<String> chavesComPendentes(long idadeMinimaMs);
}
