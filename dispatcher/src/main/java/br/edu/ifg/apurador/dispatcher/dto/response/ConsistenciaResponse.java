package br.edu.ifg.apurador.dispatcher.dto.response;

import lombok.Builder;

@Builder
public record ConsistenciaResponse(
        long eventos,
        long pendentes,
        long aplicados,
        long rejeitados,
        long superados,
        long aplicadosMaisDeUmaVez,
        int chaves,
        int chavesInconsistentes
) {
}
