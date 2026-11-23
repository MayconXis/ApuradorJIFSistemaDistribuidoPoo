package br.edu.ifg.apurador.dispatcher.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ResultadoRegistrarRequest(
        String eventoId,
        @NotBlank(message = "Informe a partida.")
        String partidaId,
        Integer placarMandante,
        Integer placarVisitante,
        List<List<Integer>> parciais,
        Boolean wo,
        String ausente,
        Integer amarelosMandante,
        Integer amarelosVisitante,
        Integer vermelhosMandante,
        Integer vermelhosVisitante,
        Integer sancoesMandante,
        Integer sancoesVisitante
) {
}
