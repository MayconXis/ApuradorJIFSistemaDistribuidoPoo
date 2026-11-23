package br.edu.ifg.apurador.dispatcher.dto.response;

import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import java.util.List;
import lombok.Builder;

@Builder
public record AuditoriaResponse(
        String chaveId,
        boolean consistente,
        List<LinhaDaTabela> gravada,
        List<LinhaDaTabela> recalculada
) {
}
