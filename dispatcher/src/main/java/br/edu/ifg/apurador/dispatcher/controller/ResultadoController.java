package br.edu.ifg.apurador.dispatcher.controller;

import static org.springframework.http.HttpStatus.ACCEPTED;

import br.edu.ifg.apurador.dispatcher.dto.request.ResultadoRegistrarRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.EventoResponse;
import br.edu.ifg.apurador.dispatcher.dto.response.ReciboResponse;
import br.edu.ifg.apurador.dispatcher.service.ResultadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ResultadoController {

    private final ResultadoService resultadoService;

    @PostMapping("/resultados")
    @ResponseStatus(ACCEPTED)
    public ReciboResponse registrar(@Valid @RequestBody ResultadoRegistrarRequest request) {
        return resultadoService.registrar(request);
    }

    @GetMapping("/eventos/{eventoId}")
    public EventoResponse detalharEvento(@PathVariable String eventoId) {
        return resultadoService.detalharEvento(eventoId);
    }
}
