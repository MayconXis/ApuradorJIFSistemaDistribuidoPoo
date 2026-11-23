package br.edu.ifg.apurador.dispatcher.service;

import br.edu.ifg.apurador.cluster.despacho.Coordenador;
import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import br.edu.ifg.apurador.core.modalidade.Modalidade;
import br.edu.ifg.apurador.dispatcher.dto.request.ChaveIncluirRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.ChaveResponse;
import br.edu.ifg.apurador.dispatcher.mapper.ChaveMapper;
import br.edu.ifg.apurador.dispatcher.validator.ChaveIncluirValidator;
import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import br.edu.ifg.apurador.persistencia.domain.PartidaEntidade;
import br.edu.ifg.apurador.persistencia.repository.ChaveRepository;
import br.edu.ifg.apurador.persistencia.repository.PartidaRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChaveIncluirService {

    private final ChaveRepository chaveRepository;
    private final PartidaRepository partidaRepository;
    private final CatalogoDeModalidades catalogoDeModalidades;
    private final Coordenador coordenador;
    private final ChaveIncluirValidator chaveIncluirValidator;

    @Transactional
    public ChaveResponse incluir(ChaveIncluirRequest request) {
        chaveIncluirValidator.validarNome(request);
        Modalidade modalidade = catalogoDeModalidades.buscar(request.modalidade());

        List<String> equipes = normalizarEquipes(request.equipes());
        chaveIncluirValidator.validarEquipes(equipes);

        String id = definirIdentificador(request.id());
        chaveIncluirValidator.validarIdentificador(id);

        ChaveEntidade chave = chaveRepository.save(ChaveMapper.toEntity(id, request, modalidade, equipes));
        gerarPartidas(id, equipes);

        String worker = coordenador.registrarChave(id);
        return ChaveMapper.toResponse(chave, worker);
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
                ? "chave-" + UUID.randomUUID().toString().substring(0, 8) : informado.trim();
    }

    private void gerarPartidas(String chaveId, List<String> equipes) {
        int numero = 1;
        for (int i = 0; i < equipes.size(); i++) {
            for (int j = i + 1; j < equipes.size(); j++) {
                partidaRepository.save(new PartidaEntidade(chaveId + "-J" + numero++, chaveId, equipes.get(i), equipes.get(j)));
            }
        }
    }
}
