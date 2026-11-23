package br.edu.ifg.apurador.dispatcher.dto.response;

import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record ClassificacaoResponse(
        String chaveId,
        String nome,
        String modalidade,
        long versao,
        Instant atualizadaEm,
        List<LinhaDaTabela> posicoes
) {
}
