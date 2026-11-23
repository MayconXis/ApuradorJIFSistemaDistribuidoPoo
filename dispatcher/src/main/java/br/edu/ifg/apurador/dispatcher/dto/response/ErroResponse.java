package br.edu.ifg.apurador.dispatcher.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        int status,
        String erro,
        Instant timestamp,
        Map<String, String> camposInvalidos
) {
}
