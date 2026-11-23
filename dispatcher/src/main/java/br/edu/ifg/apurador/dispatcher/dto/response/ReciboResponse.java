package br.edu.ifg.apurador.dispatcher.dto.response;

import lombok.Builder;

@Builder
public record ReciboResponse(
        String eventoId,
        long seq,
        boolean novo,
        String worker,
        String estado
) {
}
