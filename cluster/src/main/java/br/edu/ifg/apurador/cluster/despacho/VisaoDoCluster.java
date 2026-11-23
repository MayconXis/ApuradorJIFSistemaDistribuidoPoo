package br.edu.ifg.apurador.cluster.despacho;

import java.util.Collections;
import java.util.List;

/** Retrato do cluster para o painel: workers, teto de carga e últimas mudanças no anel. Imutável. */
public final class VisaoDoCluster {

    private final List<VisaoDoWorker> workers;
    private final int teto;
    private final int totalDeChaves;
    private final int pontosPorWorker;
    private final double folga;
    private final long tempoLimiteMs;
    private final List<String> mudancas;

    public VisaoDoCluster(List<VisaoDoWorker> workers, int teto, int totalDeChaves, int pontosPorWorker,
                          double folga, long tempoLimiteMs, List<String> mudancas) {
        this.workers = Collections.unmodifiableList(workers);
        this.teto = teto;
        this.totalDeChaves = totalDeChaves;
        this.pontosPorWorker = pontosPorWorker;
        this.folga = folga;
        this.tempoLimiteMs = tempoLimiteMs;
        this.mudancas = Collections.unmodifiableList(mudancas);
    }

    public List<VisaoDoWorker> getWorkers() {
        return workers;
    }

    public int getTeto() {
        return teto;
    }

    public int getTotalDeChaves() {
        return totalDeChaves;
    }

    public int getPontosPorWorker() {
        return pontosPorWorker;
    }

    public double getFolga() {
        return folga;
    }

    public long getTempoLimiteMs() {
        return tempoLimiteMs;
    }

    public List<String> getMudancas() {
        return mudancas;
    }
}
