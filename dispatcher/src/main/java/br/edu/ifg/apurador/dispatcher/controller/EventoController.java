package br.edu.ifg.apurador.dispatcher.controller;

import br.edu.ifg.apurador.dispatcher.dto.response.EventoResponse;
import br.edu.ifg.apurador.dispatcher.service.EventoDetalharService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoDetalharService eventoDetalharService;

    @GetMapping("/{eventoId}")
    public EventoResponse detalhar(@PathVariable String eventoId) {
        return eventoDetalharService.detalhar(eventoId);
    }
}
