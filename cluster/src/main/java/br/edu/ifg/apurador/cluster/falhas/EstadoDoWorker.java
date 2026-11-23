package br.edu.ifg.apurador.cluster.falhas;

/** Estado de um worker segundo o detector de falhas. */
public enum EstadoDoWorker {
    /** Mandou heartbeat dentro do prazo. */
    VIVO,
    /** Atrasou o heartbeat, mas ainda não passou do tempo limite. Continua no anel. */
    SUSPEITO,
    /** Passou do tempo limite sem heartbeat. Sai do anel. */
    MORTO
}
