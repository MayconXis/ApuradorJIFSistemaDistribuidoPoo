package br.edu.ifg.apurador.cluster.apoio;

import br.edu.ifg.apurador.cluster.falhas.Relogio;

/** Relógio dos testes: o tempo só anda quando o teste manda. */
public final class RelogioManual implements Relogio {

    private long agora;

    @Override
    public synchronized long agora() {
        return agora;
    }

    public synchronized void avancar(long milissegundos) {
        agora += milissegundos;
    }
}
