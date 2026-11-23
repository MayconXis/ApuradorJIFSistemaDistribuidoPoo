package br.edu.ifg.apurador.dispatcher.controller;

import br.edu.ifg.apurador.dispatcher.service.ModalidadeListarService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/modalidades")
public class ModalidadeController {

    private final ModalidadeListarService modalidadeListarService;

    @GetMapping
    public List<String> listar() {
        return modalidadeListarService.listar();
    }
}
