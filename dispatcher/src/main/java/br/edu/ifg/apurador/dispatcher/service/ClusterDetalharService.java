package br.edu.ifg.apurador.dispatcher.service;

import br.edu.ifg.apurador.cluster.despacho.Coordenador;
import br.edu.ifg.apurador.cluster.despacho.VisaoDoCluster;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClusterDetalharService {

    private final Coordenador coordenador;

    public VisaoDoCluster detalhar() {
        return coordenador.visao();
    }
}
