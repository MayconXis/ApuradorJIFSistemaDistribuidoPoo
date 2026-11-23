package br.edu.ifg.apurador.dispatcher.mapper;

import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import br.edu.ifg.apurador.dispatcher.dto.response.ClassificacaoResponse;
import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import java.util.List;

public class ClassificacaoMapper {

    private ClassificacaoMapper() {
    }

    public static ClassificacaoResponse toResponse(ChaveEntidade chave, List<LinhaDaTabela> posicoes) {
        return ClassificacaoResponse.builder()
                .chaveId(chave.getId())
                .nome(chave.getNome())
                .modalidade(chave.getModalidade())
                .versao(chave.getVersao())
                .atualizadaEm(chave.getAtualizadaEm())
                .posicoes(posicoes)
                .build();
    }
}
