package br.edu.ifg.apurador.cluster.apuracao;

import java.util.List;

/**
 * O que o aplicador pode ler e gravar enquanto segura a trava de uma chave.
 * Tudo o que for gravado por aqui é confirmado junto, no fim da ação, ou
 * desfeito junto, se a ação falhar.
 */
public interface TransacaoDaChave {

    DadosDaChave chave();

    /** Eventos pendentes da chave, em ordem de sequência. */
    List<EventoDeResultado> pendentes();

    /** Todas as partidas da chave, com o último resultado aplicado de cada uma. */
    List<PartidaGravada> partidas();

    /** Grava o resultado na partida e marca o evento como APLICADO. */
    void aplicar(EventoDeResultado evento, EstadoDaPartida novoEstado, String worker);

    /** Encerra o evento sem mudar a partida: REJEITADO ou SUPERADO, com o motivo. */
    void encerrar(EventoDeResultado evento, EstadoDoEvento estado, String motivo, String worker);

    /** Substitui a classificação gravada da chave e avança a versão. */
    void gravarClassificacao(List<LinhaDaTabela> linhas);
}
