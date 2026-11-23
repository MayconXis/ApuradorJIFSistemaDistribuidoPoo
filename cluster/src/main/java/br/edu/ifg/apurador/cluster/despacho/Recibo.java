package br.edu.ifg.apurador.cluster.despacho;

/** Resposta do Gateway a um envio de resultado. Imutável. */
public final class Recibo {

    private final String eventoId;
    private final long seq;
    private final boolean novo;
    private final String worker;

    public Recibo(String eventoId, long seq, boolean novo, String worker) {
        this.eventoId = eventoId;
        this.seq = seq;
        this.novo = novo;
        this.worker = worker;
    }

    public String getEventoId() {
        return eventoId;
    }

    public long getSeq() {
        return seq;
    }

    /** False quando o mesmo eventoId já tinha sido gravado: o envio era repetido. */
    public boolean isNovo() {
        return novo;
    }

    /** Worker que recebeu a tarefa. Null se a tarefa ficou para a varredura. */
    public String getWorker() {
        return worker;
    }

    public Recibo comWorker(String escolhido) {
        return new Recibo(eventoId, seq, novo, escolhido);
    }
}
