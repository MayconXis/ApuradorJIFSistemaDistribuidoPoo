package br.edu.ifg.apurador.dispatcher.mapper;

import br.edu.ifg.apurador.cluster.apuracao.EventoDeResultado;
import br.edu.ifg.apurador.cluster.despacho.Recibo;
import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.dispatcher.dto.request.ResultadoRegistrarRequest;
import br.edu.ifg.apurador.dispatcher.dto.response.ReciboResponse;
import java.util.List;
import java.util.UUID;

public class ResultadoMapper {

    private ResultadoMapper() {
    }

    public static EventoDeResultado toEvento(ResultadoRegistrarRequest request) {
        String eventoId = request.eventoId() == null || request.eventoId().isBlank()
                ? UUID.randomUUID().toString() : request.eventoId().trim();
        Lado ausente = null;
        if (request.ausente() != null && !request.ausente().isBlank()) {
            ausente = Lado.valueOf(request.ausente().trim().toUpperCase());
        }
        return new EventoDeResultado(eventoId, 0, null, request.partidaId().trim(),
                zeroSeNulo(request.placarMandante()), zeroSeNulo(request.placarVisitante()), toMatriz(request.parciais()),
                Boolean.TRUE.equals(request.wo()), ausente,
                new int[] {zeroSeNulo(request.amarelosMandante()), zeroSeNulo(request.amarelosVisitante())},
                new int[] {zeroSeNulo(request.vermelhosMandante()), zeroSeNulo(request.vermelhosVisitante())},
                new int[] {zeroSeNulo(request.sancoesMandante()), zeroSeNulo(request.sancoesVisitante())});
    }

    public static ReciboResponse toResponse(Recibo recibo) {
        return ReciboResponse.builder()
                .eventoId(recibo.getEventoId())
                .seq(recibo.getSeq())
                .novo(recibo.isNovo())
                .worker(recibo.getWorker())
                .estado("PENDENTE")
                .build();
    }

    private static int zeroSeNulo(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private static int[][] toMatriz(List<List<Integer>> parciais) {
        if (parciais == null) {
            return new int[0][2];
        }
        int[][] matriz = new int[parciais.size()][2];
        for (int i = 0; i < parciais.size(); i++) {
            matriz[i][0] = parciais.get(i).get(0);
            matriz[i][1] = parciais.get(i).get(1);
        }
        return matriz;
    }
}
