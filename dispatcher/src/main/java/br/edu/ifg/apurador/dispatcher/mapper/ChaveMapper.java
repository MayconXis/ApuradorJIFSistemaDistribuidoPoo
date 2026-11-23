package br.edu.ifg.apurador.dispatcher.mapper;

import br.edu.ifg.apurador.core.modalidade.Modalidade;
import br.edu.ifg.apurador.dispatcher.dto.request.ChaveIncluirRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.ChaveResponse;
import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import java.util.ArrayList;
import java.util.List;

public class ChaveMapper {

    private ChaveMapper() {
    }

    public static ChaveEntidade toEntity(String id, ChaveIncluirRequest request, Modalidade modalidade, List<String> equipes) {
        return new ChaveEntidade(id, request.nome().trim(), modalidade.getCodigo(), equipes);
    }

    public static ChaveResponse toResponse(ChaveEntidade chave, String worker) {
        return ChaveResponse.builder()
                .id(chave.getId())
                .nome(chave.getNome())
                .modalidade(chave.getModalidade())
                .equipes(new ArrayList<>(chave.getEquipes()))
                .versao(chave.getVersao())
                .worker(worker)
                .build();
    }
}
