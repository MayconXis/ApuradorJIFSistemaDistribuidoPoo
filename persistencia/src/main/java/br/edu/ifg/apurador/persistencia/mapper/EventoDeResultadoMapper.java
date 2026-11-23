package br.edu.ifg.apurador.persistencia.mapper;

import br.edu.ifg.apurador.cluster.apuracao.EventoDeResultado;
import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.persistencia.domain.DadosDoResultado;
import br.edu.ifg.apurador.persistencia.domain.EventoEntidade;

public class EventoDeResultadoMapper {

    private EventoDeResultadoMapper() {
    }

    public static EventoDeResultado toEvento(EventoEntidade e) {
        return montar(e.getEventoId(), e.getSeq(), e.getChaveId(), e.getPartidaId(), e);
    }

    public static EventoDeResultado montar(String eventoId, long seq, String chaveId, String partidaId, DadosDoResultado d) {
        return new EventoDeResultado(eventoId, seq, chaveId, partidaId, d.getPlacarMandante(), d.getPlacarVisitante(),
                ParciaisMapper.toMatriz(d.getParciais()), d.isWo(), d.getAusente() == null ? null : Lado.valueOf(d.getAusente()),
                new int[] {d.getAmarelosMandante(), d.getAmarelosVisitante()},
                new int[] {d.getVermelhosMandante(), d.getVermelhosVisitante()},
                new int[] {d.getSancoesMandante(), d.getSancoesVisitante()});
    }

    public static void preencher(DadosDoResultado destino, EventoDeResultado evento) {
        destino.setPlacarMandante(evento.getPlacarMandante());
        destino.setPlacarVisitante(evento.getPlacarVisitante());
        destino.setParciais(ParciaisMapper.toTexto(evento.getParciais()));
        destino.setWo(evento.isWo());
        destino.setAusente(evento.getAusente() == null ? null : evento.getAusente().name());
        destino.setAmarelosMandante(evento.amarelos(Lado.MANDANTE));
        destino.setAmarelosVisitante(evento.amarelos(Lado.VISITANTE));
        destino.setVermelhosMandante(evento.vermelhos(Lado.MANDANTE));
        destino.setVermelhosVisitante(evento.vermelhos(Lado.VISITANTE));
        destino.setSancoesMandante(evento.sancoes(Lado.MANDANTE));
        destino.setSancoesVisitante(evento.sancoes(Lado.VISITANTE));
    }
}
