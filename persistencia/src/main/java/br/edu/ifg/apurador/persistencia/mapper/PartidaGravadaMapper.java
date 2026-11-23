package br.edu.ifg.apurador.persistencia.mapper;

import br.edu.ifg.apurador.cluster.apuracao.EventoDeResultado;
import br.edu.ifg.apurador.cluster.apuracao.PartidaGravada;
import br.edu.ifg.apurador.persistencia.domain.PartidaEntidade;

public class PartidaGravadaMapper {

    private PartidaGravadaMapper() {
    }

    public static PartidaGravada toGravada(PartidaEntidade p) {
        EventoDeResultado resultado = p.getEventoId() == null ? null
                : EventoDeResultadoMapper.montar(p.getEventoId(), p.getSeqAplicada(), p.getChaveId(), p.getId(), p);
        return new PartidaGravada(p.getId(), p.getChaveId(), p.getMandante(), p.getVisitante(), p.getEstado(), resultado);
    }
}
