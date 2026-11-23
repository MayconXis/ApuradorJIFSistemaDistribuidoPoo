package br.edu.ifg.apurador.dispatcher.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record ChaveResponse(
        String id,
        String nome,
        String modalidade,
        List<String> equipes,
        long versao,
        String worker
) {
}
