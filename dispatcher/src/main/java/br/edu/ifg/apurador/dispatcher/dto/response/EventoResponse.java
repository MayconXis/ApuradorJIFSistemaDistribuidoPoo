package br.edu.ifg.apurador.dispatcher.dto.response;

import java.time.Instant;
import lombok.Builder;

@Builder
public record EventoResponse(
        String eventoId,
        long seq,
        String chaveId,
        String partidaId,
        String estado,
        String worker,
        String motivo,
        Instant recebidoEm,
        Instant encerradoEm
) {
}
