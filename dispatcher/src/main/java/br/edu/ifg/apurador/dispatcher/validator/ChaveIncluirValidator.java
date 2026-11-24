package br.edu.ifg.apurador.dispatcher.validator;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import br.edu.ifg.apurador.persistencia.repository.ChaveRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class ChaveIncluirValidator {

    private static final int MINIMO_DE_EQUIPES = 3;
    private static final int MAXIMO_DE_EQUIPES = 5;

    private final ChaveRepository chaveRepository;

    public void validarEquipes(List<String> equipes) {
        if (equipes.size() < MINIMO_DE_EQUIPES || equipes.size() > MAXIMO_DE_EQUIPES) {
            throw new ResponseStatusException(BAD_REQUEST, "A chave deve ter de 3 a 5 equipes com nomes diferentes.");
        }
    }

    public void validarIdentificador(String id) {
        if (chaveRepository.existsById(id)) {
            throw new ResponseStatusException(BAD_REQUEST, "Já existe uma chave com o identificador " + id + ".");
        }
    }
}
