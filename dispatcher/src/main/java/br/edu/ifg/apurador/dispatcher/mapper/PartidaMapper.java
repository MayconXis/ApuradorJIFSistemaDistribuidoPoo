package br.edu.ifg.apurador.dispatcher.mapper;

import br.edu.ifg.apurador.dispatcher.dto.response.PartidaResponse;
import br.edu.ifg.apurador.persistencia.domain.PartidaEntidade;

public class PartidaMapper {

    private PartidaMapper() {
    }

    public static PartidaResponse toResponse(PartidaEntidade partida) {
        return PartidaResponse.builder()
                .id(partida.getId())
                .chaveId(partida.getChaveId())
                .mandante(partida.getMandante())
                .visitante(partida.getVisitante())
                .estado(partida.getEstado().name())
                .placarMandante(partida.getPlacarMandante())
                .placarVisitante(partida.getPlacarVisitante())
                .parciais(partida.getParciais())
                .wo(partida.isWo())
                .ausente(partida.getAusente())
                .build();
    }
}
