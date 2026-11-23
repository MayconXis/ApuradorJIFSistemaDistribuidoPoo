package br.edu.ifg.apurador.dispatcher.mapper;

import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import br.edu.ifg.apurador.dispatcher.dto.response.AuditoriaResponse;
import java.util.List;

public class AuditoriaMapper {

    private AuditoriaMapper() {
    }

    public static AuditoriaResponse toResponse(String chaveId, boolean consistente, List<LinhaDaTabela> gravada,
                                               List<LinhaDaTabela> recalculada) {
        return AuditoriaResponse.builder()
                .chaveId(chaveId)
                .consistente(consistente)
                .gravada(gravada)
                .recalculada(recalculada)
                .build();
    }
}
