package br.edu.ifg.apurador.cluster.falhas;

/**
 * Fonte de tempo, em milissegundos. Existe para o detector de falhas poder ser
 * testado com um relógio manual, sem esperar o tempo passar de verdade.
 */
public interface Relogio {

    long agora();
}
