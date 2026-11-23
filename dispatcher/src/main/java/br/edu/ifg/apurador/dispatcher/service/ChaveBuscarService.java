package br.edu.ifg.apurador.dispatcher.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import br.edu.ifg.apurador.persistencia.repository.ChaveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChaveBuscarService {

    private final ChaveRepository chaveRepository;

    public ChaveEntidade buscar(String chaveId) {
        return chaveRepository.findById(chaveId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Chave desconhecida: " + chaveId));
    }
}
