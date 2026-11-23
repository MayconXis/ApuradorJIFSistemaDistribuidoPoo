package br.edu.ifg.apurador.dispatcher.controller;

import static org.springframework.http.HttpStatus.NO_CONTENT;

import br.edu.ifg.apurador.cluster.despacho.VisaoDoCluster;
import br.edu.ifg.apurador.dispatcher.dto.request.HeartbeatReceberRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.ConsistenciaResponse;
import br.edu.ifg.apurador.dispatcher.service.ClusterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ClusterController {

    private final ClusterService clusterService;

    @GetMapping("/api/cluster")
    public VisaoDoCluster detalhar() {
        return clusterService.detalhar();
    }

    @GetMapping("/api/consistencia")
    public ConsistenciaResponse detalharConsistencia() {
        return clusterService.detalharConsistencia();
    }

    @PostMapping("/interno/heartbeats")
    @ResponseStatus(NO_CONTENT)
    public void receberHeartbeat(@Valid @RequestBody HeartbeatReceberRequest request) {
        clusterService.receberHeartbeat(request);
    }
}
