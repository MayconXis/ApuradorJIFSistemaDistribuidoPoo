package br.edu.ifg.apurador.cluster.falhas;

/** Mudança de estado de um worker, percebida pelo detector. Imutável. */
public final class Transicao {

    private final String worker;
    private final EstadoDoWorker de;
    private final EstadoDoWorker para;
    private final long instante;

    public Transicao(String worker, EstadoDoWorker de, EstadoDoWorker para, long instante) {
        this.worker = worker;
        this.de = de;
        this.para = para;
        this.instante = instante;
    }

    public String getWorker() {
        return worker;
    }

    /** Estado anterior. Null quando é o primeiro heartbeat do worker. */
    public EstadoDoWorker getDe() {
        return de;
    }

    public EstadoDoWorker getPara() {
        return para;
    }

    public long getInstante() {
        return instante;
    }

    @Override
    public String toString() {
        return worker + ": " + (de == null ? "novo" : de) + " -> " + para;
    }
}
