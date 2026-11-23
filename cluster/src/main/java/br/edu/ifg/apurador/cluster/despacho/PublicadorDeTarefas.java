package br.edu.ifg.apurador.cluster.despacho;

/**
 * Porta de saída do Dispatcher para o middleware. A implementação de produção
 * publica no RabbitMQ, na fila do worker. Nos testes, uma fila em memória.
 */
public interface PublicadorDeTarefas {

    /** Publica a tarefa na fila do worker. Lança exceção se a publicação não foi confirmada. */
    void publicar(String worker, Tarefa tarefa);
}
