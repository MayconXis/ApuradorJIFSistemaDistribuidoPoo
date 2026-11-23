package br.edu.ifg.apurador.dispatcher.service;

import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import br.edu.ifg.apurador.dispatcher.dto.response.ClassificacaoResponse;
import br.edu.ifg.apurador.dispatcher.mapper.ClassificacaoMapper;
import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import br.edu.ifg.apurador.persistencia.domain.PosicaoEntidade;
import br.edu.ifg.apurador.persistencia.repository.PosicaoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassificacaoDetalharService {

    private final PosicaoRepository posicaoRepository;
    private final ChaveBuscarService chaveBuscarService;

    @Transactional(readOnly = true)
    public ClassificacaoResponse detalhar(String chaveId) {
        ChaveEntidade chave = chaveBuscarService.buscar(chaveId);
        List<LinhaDaTabela> posicoes = posicaoRepository.findByChaveIdOrderByColocacaoAsc(chaveId).stream()
                .map(PosicaoEntidade::paraLinha)
                .toList();
        return ClassificacaoMapper.toResponse(chave, posicoes);
    }
}
