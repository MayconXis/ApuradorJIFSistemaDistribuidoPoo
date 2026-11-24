package br.edu.ifg.apurador.dispatcher.service;

import br.edu.ifg.apurador.cluster.despacho.Coordenador;
import br.edu.ifg.apurador.dispatcher.dto.request.HeartbeatReceberRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HeartbeatReceberService {

    private final Coordenador coordenador;

    public void receber(HeartbeatReceberRequest request) {
        coordenador.receberHeartbeat(request.workerId().trim());
    }
}
