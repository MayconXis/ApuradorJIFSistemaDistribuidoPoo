package br.edu.ifg.apurador.cluster.apuracao;

/**
 * Porta do worker para o histórico. A implementação de produção usa JPA e
 * PostgreSQL. Nos testes, um histórico em memória.
 */
public interface HistoricoDaChave {

    /**
     * Executa a ação dentro de uma transação que segura a trava da chave.
     * Enquanto a ação roda, nenhum outro worker aplica eventos da mesma chave.
     * Se a ação lançar exceção, nada do que ela gravou é confirmado.
     */
    <T> T comTrava(String chaveId, AcaoNaChave<T> acao);
}
