package br.edu.ifg.apurador.dispatcher.controller;

import static org.springframework.http.HttpStatus.CREATED;

import br.edu.ifg.apurador.dispatcher.dto.request.ChaveIncluirRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.AuditoriaResponse;
import br.edu.ifg.apurador.dispatcher.dto.response.ChaveResponse;
import br.edu.ifg.apurador.dispatcher.dto.response.ClassificacaoResponse;
import br.edu.ifg.apurador.dispatcher.dto.response.PartidaResponse;
import br.edu.ifg.apurador.dispatcher.service.ChaveService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/chaves")
public class ChaveController {

    private final ChaveService chaveService;

    @PostMapping
    @ResponseStatus(CREATED)
    public ChaveResponse incluir(@Valid @RequestBody ChaveIncluirRequest request) {
        return chaveService.incluir(request);
    }

    @GetMapping
    public List<ChaveResponse> listar() {
        return chaveService.listar();
    }

    @GetMapping("/{id}/partidas")
    public List<PartidaResponse> listarPartidas(@PathVariable String id) {
        return chaveService.listarPartidas(id);
    }

    @GetMapping("/{id}/classificacao")
    public ClassificacaoResponse detalharClassificacao(@PathVariable String id) {
        return chaveService.detalharClassificacao(id);
    }

    @GetMapping("/{id}/auditoria")
    public AuditoriaResponse auditar(@PathVariable String id) {
        return chaveService.auditar(id);
    }
}
