package br.edu.ifg.apurador.dispatcher.service;

import br.edu.ifg.apurador.dispatcher.dto.response.PartidaResponse;
import br.edu.ifg.apurador.dispatcher.mapper.PartidaMapper;
import br.edu.ifg.apurador.persistencia.repository.PartidaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartidaListarService {

    private final PartidaRepository partidaRepository;
    private final ChaveBuscarService chaveBuscarService;

    @Transactional(readOnly = true)
    public List<PartidaResponse> listar(String chaveId) {
        chaveBuscarService.buscar(chaveId);
        return partidaRepository.findByChaveIdOrderByIdAsc(chaveId).stream()
                .map(PartidaMapper::toResponse)
                .toList();
    }
}
