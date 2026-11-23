package br.edu.ifg.apurador.dispatcher.service;

import br.edu.ifg.apurador.cluster.apuracao.EstadoDoEvento;
import br.edu.ifg.apurador.cluster.despacho.Coordenador;
import br.edu.ifg.apurador.cluster.despacho.VisaoDoCluster;
import br.edu.ifg.apurador.dispatcher.dto.request.HeartbeatReceberRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.ConsistenciaResponse;
import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import br.edu.ifg.apurador.persistencia.repository.ChaveRepository;
import br.edu.ifg.apurador.persistencia.repository.EventoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final Coordenador coordenador;
    private final ChaveRepository chaveRepository;
    private final EventoRepository eventoRepository;
    private final ChaveService chaveService;

    public VisaoDoCluster detalhar() {
        return coordenador.visao();
    }

    public void receberHeartbeat(HeartbeatReceberRequest request) {
        coordenador.receberHeartbeat(request.workerId().trim());
    }

    @Transactional(readOnly = true)
    public ConsistenciaResponse detalharConsistencia() {
        List<ChaveEntidade> chaves = chaveRepository.findAll();
        int inconsistentes = 0;
        for (ChaveEntidade chave : chaves) {
            if (!chaveService.auditar(chave.getId()).consistente()) {
                inconsistentes++;
            }
        }
        return ConsistenciaResponse.builder()
                .eventos(eventoRepository.count())
                .pendentes(eventoRepository.countByEstado(EstadoDoEvento.PENDENTE))
                .aplicados(eventoRepository.countByEstado(EstadoDoEvento.APLICADO))
                .rejeitados(eventoRepository.countByEstado(EstadoDoEvento.REJEITADO))
                .superados(eventoRepository.countByEstado(EstadoDoEvento.SUPERADO))
                .aplicadosMaisDeUmaVez(eventoRepository.contarAplicadosMaisDeUmaVez())
                .chaves(chaves.size())
                .chavesInconsistentes(inconsistentes)
                .build();
    }
}
