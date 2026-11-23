package br.edu.ifg.apurador.dispatcher.controller;

import static org.springframework.http.HttpStatus.ACCEPTED;

import br.edu.ifg.apurador.dispatcher.dto.request.ResultadoRegistrarRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.ReciboResponse;
import br.edu.ifg.apurador.dispatcher.service.ResultadoRegistrarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resultados")
public class ResultadoController {

    private final ResultadoRegistrarService resultadoRegistrarService;

    @PostMapping
    @ResponseStatus(ACCEPTED)
    public ReciboResponse registrar(@RequestBody ResultadoRegistrarRequest request) {
        return resultadoRegistrarService.registrar(request);
    }
}
