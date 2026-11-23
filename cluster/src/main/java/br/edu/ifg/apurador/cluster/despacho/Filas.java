package br.edu.ifg.apurador.cluster.despacho;

/**
 * Nomes usados no RabbitMQ, combinados entre o Dispatcher e os workers.
 *
 * Cada worker tem a sua fila, e essa fila é a partição: o Dispatcher publica
 * no exchange direto usando o identificador do worker como rota.
 */
public final class Filas {

    /** Exchange direto das tarefas. A rota é o identificador do worker. */
    public static final String EXCHANGE_TAREFAS = "apuracao.tarefas";
    /** Exchange para onde vai a mensagem que passou do limite de entregas. */
    public static final String EXCHANGE_MORTAS = "apuracao.dlx";
    /** Fila das mensagens mortas (DLQ). */
    public static final String FILA_MORTAS = "apuracao.dlq";
    /** Cabeçalho da mensagem com o motivo da tarefa. */
    public static final String CABECALHO_MOTIVO = "motivo";

    private Filas() {
    }

    /** Nome da fila do worker, por exemplo apuracao.particao.worker-0. */
    public static String filaDe(String worker) {
        return "apuracao.particao." + worker;
    }
}
