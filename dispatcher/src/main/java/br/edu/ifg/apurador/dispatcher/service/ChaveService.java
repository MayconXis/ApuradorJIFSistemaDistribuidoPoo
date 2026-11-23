package br.edu.ifg.apurador.dispatcher.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import br.edu.ifg.apurador.cluster.apuracao.CalculoDaTabela;
import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import br.edu.ifg.apurador.cluster.apuracao.PartidaGravada;
import br.edu.ifg.apurador.cluster.despacho.Coordenador;
import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import br.edu.ifg.apurador.core.modalidade.Modalidade;
import br.edu.ifg.apurador.dispatcher.dto.request.ChaveIncluirRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.AuditoriaResponse;
import br.edu.ifg.apurador.dispatcher.dto.response.ChaveResponse;
import br.edu.ifg.apurador.dispatcher.dto.response.ClassificacaoResponse;
import br.edu.ifg.apurador.dispatcher.dto.response.PartidaResponse;
import br.edu.ifg.apurador.dispatcher.mapper.AuditoriaMapper;
import br.edu.ifg.apurador.dispatcher.mapper.ChaveMapper;
import br.edu.ifg.apurador.dispatcher.mapper.ClassificacaoMapper;
import br.edu.ifg.apurador.dispatcher.mapper.PartidaMapper;
import br.edu.ifg.apurador.dispatcher.validator.ChaveValidator;
import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import br.edu.ifg.apurador.persistencia.domain.PartidaEntidade;
import br.edu.ifg.apurador.persistencia.domain.PosicaoEntidade;
import br.edu.ifg.apurador.persistencia.mapper.ChaveDadosMapper;
import br.edu.ifg.apurador.persistencia.mapper.PartidaGravadaMapper;
import br.edu.ifg.apurador.persistencia.repository.ChaveRepository;
import br.edu.ifg.apurador.persistencia.repository.PartidaRepository;
import br.edu.ifg.apurador.persistencia.repository.PosicaoRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChaveService {

    private final ChaveRepository chaveRepository;
    private final PartidaRepository partidaRepository;
    private final PosicaoRepository posicaoRepository;
    private final CatalogoDeModalidades catalogoDeModalidades;
    private final Coordenador coordenador;
    private final CalculoDaTabela calculoDaTabela;
    private final ChaveValidator chaveValidator;

    @Transactional
    public ChaveResponse incluir(ChaveIncluirRequest request) {
        Modalidade modalidade = catalogoDeModalidades.buscar(request.modalidade());

        List<String> equipes = normalizarEquipes(request.equipes());
        chaveValidator.validarEquipes(equipes);

        String id = definirIdentificador(request.id());
        chaveValidator.validarIdentificador(id);

        ChaveEntidade chave = chaveRepository.save(ChaveMapper.toEntity(id, request, modalidade, equipes));
        gerarPartidas(id, equipes);

        String worker = coordenador.registrarChave(id);
        return ChaveMapper.toResponse(chave, worker);
    }

    @Transactional(readOnly = true)
    public List<ChaveResponse> listar() {
        return chaveRepository.findAllByOrderByCriadaEmAscIdAsc().stream()
                .map(chave -> ChaveMapper.toResponse(chave, coordenador.registrarChave(chave.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PartidaResponse> listarPartidas(String chaveId) {
        buscarEntidade(chaveId);
        return partidaRepository.findByChaveIdOrderByIdAsc(chaveId).stream()
                .map(PartidaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClassificacaoResponse detalharClassificacao(String chaveId) {
        ChaveEntidade chave = buscarEntidade(chaveId);
        List<LinhaDaTabela> posicoes = posicaoRepository.findByChaveIdOrderByColocacaoAsc(chaveId).stream()
                .map(PosicaoEntidade::paraLinha)
                .toList();
        return ClassificacaoMapper.toResponse(chave, posicoes);
    }

    @Transactional(readOnly = true)
    public AuditoriaResponse auditar(String chaveId) {
        ChaveEntidade chave = buscarEntidade(chaveId);

        List<PartidaGravada> partidas = partidaRepository.findByChaveIdOrderByIdAsc(chaveId).stream()
                .map(PartidaGravadaMapper::toGravada)
                .toList();
        List<LinhaDaTabela> tabelaGravada = posicaoRepository.findByChaveIdOrderByColocacaoAsc(chaveId).stream()
                .map(PosicaoEntidade::paraLinha)
                .toList();

        if (partidas.stream().noneMatch(PartidaGravada::temResultado)) {
            return AuditoriaMapper.toResponse(chaveId, tabelaGravada.isEmpty(), tabelaGravada, List.of());
        }

        List<LinhaDaTabela> recalculada = calculoDaTabela.calcular(
                ChaveDadosMapper.toDados(chave),
                partidas,
                catalogoDeModalidades.buscar(chave.getModalidade()));
        return AuditoriaMapper.toResponse(chaveId, recalculada.equals(tabelaGravada), tabelaGravada, recalculada);
    }

    public ChaveEntidade buscarEntidade(String chaveId) {
        return chaveRepository.findById(chaveId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Chave desconhecida: " + chaveId));
    }

    private List<String> normalizarEquipes(List<String> informadas) {
        Set<String> equipes = new LinkedHashSet<>();
        if (informadas != null) {
            for (String equipe : informadas) {
                if (equipe != null && !equipe.isBlank()) {
                    equipes.add(equipe.trim());
                }
            }
        }
        return new ArrayList<>(equipes);
    }

    private String definirIdentificador(String informado) {
        return informado == null || informado.isBlank()
                ? "chave-" + UUID.randomUUID().toString().substring(0, 8)
                : informado.trim();
    }

    private void gerarPartidas(String chaveId, List<String> equipes) {
        int numero = 1;
        for (int i = 0; i < equipes.size(); i++) {
            for (int j = i + 1; j < equipes.size(); j++) {
                partidaRepository.save(
                        new PartidaEntidade(chaveId + "-J" + numero++, chaveId, equipes.get(i), equipes.get(j)));
            }
        }
    }
}
