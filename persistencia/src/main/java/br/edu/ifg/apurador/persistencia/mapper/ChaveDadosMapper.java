package br.edu.ifg.apurador.persistencia.mapper;

import br.edu.ifg.apurador.cluster.apuracao.DadosDaChave;
import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;

public class ChaveDadosMapper {

    private ChaveDadosMapper() {
    }

    public static DadosDaChave toDados(ChaveEntidade chave) {
        return new DadosDaChave(chave.getId(), chave.getNome(), chave.getModalidade(), chave.getEquipes());
    }
}
