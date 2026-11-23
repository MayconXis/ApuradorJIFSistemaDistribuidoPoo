package br.edu.ifg.apurador.cluster.despacho;

import br.edu.ifg.apurador.cluster.falhas.EstadoDoWorker;
import java.util.Collections;
import java.util.List;

/** Retrato de um worker para o painel do cluster. Imutável. */
public final class VisaoDoWorker {

    private final String id;
    private final EstadoDoWorker estado;
    private final long silencioMs;
    private final List<String> chaves;

    public VisaoDoWorker(String id, EstadoDoWorker estado, long silencioMs, List<String> chaves) {
        this.id = id;
        this.estado = estado;
        this.silencioMs = silencioMs;
        this.chaves = Collections.unmodifiableList(chaves);
    }

    public String getId() {
        return id;
    }

    public EstadoDoWorker getEstado() {
        return estado;
    }

    /** Milissegundos desde o último heartbeat. */
    public long getSilencioMs() {
        return silencioMs;
    }

    public List<String> getChaves() {
        return chaves;
    }

    public int getCarga() {
        return chaves.size();
    }
}
