package br.edu.ifg.apurador.dispatcher.dto.request;

import java.util.List;

public record ChaveIncluirRequest(
        String id,
        String nome,
        String modalidade,
        List<String> equipes
) {
}
