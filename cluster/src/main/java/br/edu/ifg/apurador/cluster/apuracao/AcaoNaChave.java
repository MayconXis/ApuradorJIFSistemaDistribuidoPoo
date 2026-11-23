package br.edu.ifg.apurador.cluster.apuracao;

/** Bloco de código executado com a chave travada. */
public interface AcaoNaChave<T> {

    T executar(TransacaoDaChave transacao);
}
