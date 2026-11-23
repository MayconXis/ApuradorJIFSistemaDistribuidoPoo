package br.edu.ifg.apurador.dispatcher.dto.response;

import lombok.Builder;

@Builder
public record PartidaResponse(
        String id,
        String chaveId,
        String mandante,
        String visitante,
        String estado,
        int placarMandante,
        int placarVisitante,
        String parciais,
        boolean wo,
        String ausente
) {
}
