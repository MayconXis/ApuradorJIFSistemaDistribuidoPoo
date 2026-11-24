package br.edu.ifg.apurador.dispatcher.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ChaveIncluirRequest(
        String id,
        @NotBlank(message = "Informe o nome da chave.") String nome,
        @NotBlank(message = "Informe a modalidade.") String modalidade,
        @NotEmpty(message = "Informe de 3 a 5 equipes para a chave.") List<String> equipes
) {
}
