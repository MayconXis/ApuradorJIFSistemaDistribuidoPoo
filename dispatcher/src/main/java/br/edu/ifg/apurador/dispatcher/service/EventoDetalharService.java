package br.edu.ifg.apurador.dispatcher.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import br.edu.ifg.apurador.dispatcher.dto.response.EventoResponse;
import br.edu.ifg.apurador.dispatcher.mapper.EventoMapper;
import br.edu.ifg.apurador.persistencia.domain.EventoEntidade;
import br.edu.ifg.apurador.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EventoDetalharService {

    private final EventoRepository eventoRepository;

    @Transactional(readOnly = true)
    public EventoResponse detalhar(String eventoId) {
        EventoEntidade evento = eventoRepository.findByEventoId(eventoId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Evento desconhecido: " + eventoId));
        return EventoMapper.toResponse(evento);
    }
}
