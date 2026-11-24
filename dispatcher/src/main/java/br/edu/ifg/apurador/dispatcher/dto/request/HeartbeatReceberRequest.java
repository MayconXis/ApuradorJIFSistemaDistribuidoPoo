package br.edu.ifg.apurador.dispatcher.dto.request;

import jakarta.validation.constraints.NotBlank;

public record HeartbeatReceberRequest(
        @NotBlank(message = "Informe o identificador do worker.") String workerId
) {
}
