package br.edu.ifg.apurador.dispatcher.validator;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import br.edu.ifg.apurador.dispatcher.dto.request.ResultadoRegistrarRequest;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ResultadoRegistrarValidator {

    public void validar(ResultadoRegistrarRequest request) {
        if (request.parciais() != null) {
            for (List<Integer> set : request.parciais()) {
                if (set == null || set.size() != 2 || set.get(0) == null || set.get(1) == null) {
                    throw new ResponseStatusException(BAD_REQUEST, "Cada set precisa de dois valores.");
                }
            }
        }
    }
}
