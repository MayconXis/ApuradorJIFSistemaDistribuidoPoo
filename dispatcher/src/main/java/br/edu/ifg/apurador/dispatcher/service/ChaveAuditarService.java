package br.edu.ifg.apurador.dispatcher.service;

import br.edu.ifg.apurador.cluster.apuracao.CalculoDaTabela;
import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import br.edu.ifg.apurador.cluster.apuracao.PartidaGravada;
import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import br.edu.ifg.apurador.dispatcher.dto.response.AuditoriaResponse;
import br.edu.ifg.apurador.dispatcher.mapper.AuditoriaMapper;
import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import br.edu.ifg.apurador.persistencia.domain.PosicaoEntidade;
import br.edu.ifg.apurador.persistencia.mapper.ChaveDadosMapper;
import br.edu.ifg.apurador.persistencia.mapper.PartidaGravadaMapper;
import br.edu.ifg.apurador.persistencia.repository.PartidaRepository;
import br.edu.ifg.apurador.persistencia.repository.PosicaoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChaveAuditarService {

    private final PartidaRepository partidaRepository;
    private final PosicaoRepository posicaoRepository;
    private final CatalogoDeModalidades catalogoDeModalidades;
    private final CalculoDaTabela calculoDaTabela;
    private final ChaveBuscarService chaveBuscarService;

    @Transactional(readOnly = true)
    public AuditoriaResponse auditar(String chaveId) {
        ChaveEntidade chave = chaveBuscarService.buscar(chaveId);

        List<PartidaGravada> partidas = partidaRepository.findByChaveIdOrderByIdAsc(chaveId).stream()
                .map(PartidaGravadaMapper::toGravada)
                .toList();
        List<LinhaDaTabela> tabelaGravada = posicaoRepository.findByChaveIdOrderByColocacaoAsc(chaveId).stream()
                .map(PosicaoEntidade::paraLinha)
                .toList();

        if (partidas.stream().noneMatch(PartidaGravada::temResultado)) {
            return AuditoriaMapper.toResponse(chaveId, tabelaGravada.isEmpty(), tabelaGravada, List.of());
        }

        List<LinhaDaTabela> recalculada = calculoDaTabela.calcular(ChaveDadosMapper.toDados(chave), partidas,
                catalogoDeModalidades.buscar(chave.getModalidade()));
        return AuditoriaMapper.toResponse(chaveId, recalculada.equals(tabelaGravada), tabelaGravada, recalculada);
    }
}
