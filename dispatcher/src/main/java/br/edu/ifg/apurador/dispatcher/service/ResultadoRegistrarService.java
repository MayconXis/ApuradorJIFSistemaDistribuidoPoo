package br.edu.ifg.apurador.dispatcher.service;

import br.edu.ifg.apurador.cluster.despacho.Recibo;
import br.edu.ifg.apurador.cluster.despacho.RegistradorDeResultados;
import br.edu.ifg.apurador.dispatcher.dto.request.ResultadoRegistrarRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.ReciboResponse;
import br.edu.ifg.apurador.dispatcher.mapper.ResultadoMapper;
import br.edu.ifg.apurador.dispatcher.validator.ResultadoRegistrarValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultadoRegistrarService {

    private final RegistradorDeResultados registradorDeResultados;
    private final ResultadoRegistrarValidator resultadoRegistrarValidator;

    public ReciboResponse registrar(ResultadoRegistrarRequest request) {
        resultadoRegistrarValidator.validar(request);

        Recibo recibo = registradorDeResultados.registrar(ResultadoMapper.toEvento(request));
        log.info("evento={} seq={} novo={} worker={}", recibo.getEventoId(), recibo.getSeq(), recibo.isNovo(), recibo.getWorker());

        return ResultadoMapper.toResponse(recibo);
    }
}
