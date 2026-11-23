package br.edu.ifg.apurador.dispatcher.controller;

import br.edu.ifg.apurador.dispatcher.service.ModalidadeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/modalidades")
public class ModalidadeController {

    private final ModalidadeService modalidadeService;

    @GetMapping
    public List<String> listar() {
        return modalidadeService.listar();
    }
}
