package br.edu.ifg.apurador.dispatcher.controller;

import br.edu.ifg.apurador.cluster.despacho.VisaoDoCluster;
import br.edu.ifg.apurador.dispatcher.service.ClusterDetalharService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cluster")
public class ClusterController {

    private final ClusterDetalharService clusterDetalharService;

    @GetMapping
    public VisaoDoCluster detalhar() {
        return clusterDetalharService.detalhar();
    }
}
