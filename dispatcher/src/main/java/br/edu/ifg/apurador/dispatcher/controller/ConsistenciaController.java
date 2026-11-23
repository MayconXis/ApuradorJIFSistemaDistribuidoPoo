package br.edu.ifg.apurador.dispatcher.controller;

import br.edu.ifg.apurador.dispatcher.dto.response.ConsistenciaResponse;
import br.edu.ifg.apurador.dispatcher.service.ConsistenciaDetalharService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consistencia")
public class ConsistenciaController {

    private final ConsistenciaDetalharService consistenciaDetalharService;

    @GetMapping
    public ConsistenciaResponse detalhar() {
        return consistenciaDetalharService.detalhar();
    }
}
