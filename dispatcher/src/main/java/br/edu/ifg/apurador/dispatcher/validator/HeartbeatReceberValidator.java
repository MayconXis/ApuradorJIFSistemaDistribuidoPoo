package br.edu.ifg.apurador.dispatcher.validator;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import br.edu.ifg.apurador.dispatcher.dto.request.HeartbeatReceberRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class HeartbeatReceberValidator {

    public void validar(HeartbeatReceberRequest request) {
        if (request == null || request.workerId() == null || request.workerId().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Informe o identificador do worker.");
        }
    }
}
