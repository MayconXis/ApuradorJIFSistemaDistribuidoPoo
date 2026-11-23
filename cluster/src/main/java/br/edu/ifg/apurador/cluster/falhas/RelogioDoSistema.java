package br.edu.ifg.apurador.cluster.falhas;

/** Relógio real. Usa o contador monotônico da JVM, que não volta atrás se a hora do sistema for ajustada. */
public final class RelogioDoSistema implements Relogio {

    @Override
    public long agora() {
        return System.nanoTime() / 1_000_000L;
    }
}
