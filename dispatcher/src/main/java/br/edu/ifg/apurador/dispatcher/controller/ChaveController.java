package br.edu.ifg.apurador.dispatcher.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;

import br.edu.ifg.apurador.dispatcher.dto.request.ChaveIncluirRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.AuditoriaResponse;
import br.edu.ifg.apurador.dispatcher.dto.response.ChaveResponse;
import br.edu.ifg.apurador.dispatcher.dto.response.ClassificacaoResponse;
import br.edu.ifg.apurador.dispatcher.dto.response.PartidaResponse;
import br.edu.ifg.apurador.dispatcher.service.ChaveAuditarService;
import br.edu.ifg.apurador.dispatcher.service.ChaveIncluirService;
import br.edu.ifg.apurador.dispatcher.service.ChaveListarService;
import br.edu.ifg.apurador.dispatcher.service.ClassificacaoDetalharService;
import br.edu.ifg.apurador.dispatcher.service.PartidaListarService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chaves")
public class ChaveController {

    private final ChaveIncluirService chaveIncluirService;
    private final ChaveListarService chaveListarService;
    private final PartidaListarService partidaListarService;
    private final ClassificacaoDetalharService classificacaoDetalharService;
    private final ChaveAuditarService chaveAuditarService;

    @PostMapping
    @ResponseStatus(CREATED)
    public ChaveResponse incluir(@RequestBody ChaveIncluirRequest request) {
        return chaveIncluirService.incluir(request);
    }

    @GetMapping
    public List<ChaveResponse> listar() {
        return chaveListarService.listar();
    }

    @GetMapping("/{id}/partidas")
    public List<PartidaResponse> listarPartidas(@PathVariable String id) {
        return partidaListarService.listar(id);
    }

    @GetMapping("/{id}/classificacao")
    public ResponseEntity<ClassificacaoResponse> detalharClassificacao(
            @PathVariable String id,
            @RequestHeader(value = "If-None-Match", required = false) String versaoDaTela) {
        ClassificacaoResponse response = classificacaoDetalharService.detalhar(id);
        String etag = "\"" + response.versao() + "\"";
        if (etag.equals(versaoDaTela)) {
            return ResponseEntity.status(NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(response);
    }

    @GetMapping("/{id}/auditoria")
    public AuditoriaResponse auditar(@PathVariable String id) {
        return chaveAuditarService.auditar(id);
    }
}
