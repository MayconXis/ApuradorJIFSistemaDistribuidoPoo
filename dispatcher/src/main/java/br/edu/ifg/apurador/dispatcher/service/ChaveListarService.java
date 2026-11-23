package br.edu.ifg.apurador.dispatcher.service;

import br.edu.ifg.apurador.cluster.despacho.Coordenador;
import br.edu.ifg.apurador.dispatcher.dto.response.ChaveResponse;
import br.edu.ifg.apurador.dispatcher.mapper.ChaveMapper;
import br.edu.ifg.apurador.persistencia.repository.ChaveRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChaveListarService {

    private final ChaveRepository chaveRepository;
    private final Coordenador coordenador;

    @Transactional(readOnly = true)
    public List<ChaveResponse> listar() {
        return chaveRepository.findAllByOrderByCriadaEmAscIdAsc().stream()
                .map(chave -> ChaveMapper.toResponse(chave, coordenador.registrarChave(chave.getId())))
                .toList();
    }
}
