package br.edu.ifg.apurador.dispatcher.mapper;

import br.edu.ifg.apurador.dispatcher.dto.response.EventoResponse;
import br.edu.ifg.apurador.persistencia.domain.EventoEntidade;

public class EventoMapper {

    private EventoMapper() {
    }

    public static EventoResponse toResponse(EventoEntidade evento) {
        return EventoResponse.builder()
                .eventoId(evento.getEventoId())
                .seq(evento.getSeq())
                .chaveId(evento.getChaveId())
                .partidaId(evento.getPartidaId())
                .estado(evento.getEstado().name())
                .worker(evento.getWorker())
                .motivo(evento.getMotivo())
                .recebidoEm(evento.getRecebidoEm())
                .encerradoEm(evento.getEncerradoEm())
                .build();
    }
}
