package br.edu.ifg.apurador.cluster.despacho;

import br.edu.ifg.apurador.cluster.apuracao.DadosDaChave;
import br.edu.ifg.apurador.cluster.apuracao.EventoDeResultado;
import br.edu.ifg.apurador.cluster.apuracao.PartidaGravada;

/** Porta do Gateway para o histórico: o que ele precisa para validar e gravar um envio. */
public interface RegistroDeEventos {

    /** Partida pelo identificador. Null se não existe. */
    PartidaGravada partida(String partidaId);

    /** Cadastro da chave. Null se não existe. */
    DadosDaChave chave(String chaveId);

    /**
     * Grava o evento como PENDENTE e devolve o recibo com a sequência.
     * Se o eventoId já existe, não grava de novo e devolve o recibo do evento existente.
     */
    Recibo gravarPendente(EventoDeResultado evento);
}
