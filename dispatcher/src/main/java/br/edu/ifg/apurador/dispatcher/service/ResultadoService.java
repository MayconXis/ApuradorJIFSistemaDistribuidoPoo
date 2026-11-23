package br.edu.ifg.apurador.dispatcher.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import br.edu.ifg.apurador.cluster.despacho.Recibo;
import br.edu.ifg.apurador.cluster.despacho.RegistradorDeResultados;
import br.edu.ifg.apurador.dispatcher.dto.request.ResultadoRegistrarRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.EventoResponse;
import br.edu.ifg.apurador.dispatcher.dto.response.ReciboResponse;
import br.edu.ifg.apurador.dispatcher.mapper.EventoMapper;
import br.edu.ifg.apurador.dispatcher.mapper.ResultadoMapper;
import br.edu.ifg.apurador.dispatcher.validator.ResultadoValidator;
import br.edu.ifg.apurador.persistencia.domain.EventoEntidade;
import br.edu.ifg.apurador.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultadoService {

    private final RegistradorDeResultados registradorDeResultados;
    private final EventoRepository eventoRepository;
    private final ResultadoValidator resultadoValidator;

    public ReciboResponse registrar(ResultadoRegistrarRequest request) {
        resultadoValidator.validarParciais(request);

        Recibo recibo = registradorDeResultados.registrar(ResultadoMapper.toEvento(request));
        log.info("evento={} seq={} novo={} worker={}",
                recibo.getEventoId(), recibo.getSeq(), recibo.isNovo(), recibo.getWorker());

        return ResultadoMapper.toResponse(recibo);
    }

    @Transactional(readOnly = true)
    public EventoResponse detalharEvento(String eventoId) {
        EventoEntidade evento = eventoRepository.findByEventoId(eventoId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Evento desconhecido: " + eventoId));
        return EventoMapper.toResponse(evento);
    }
}
