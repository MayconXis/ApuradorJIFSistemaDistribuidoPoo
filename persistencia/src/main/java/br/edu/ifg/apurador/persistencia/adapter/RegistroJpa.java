package br.edu.ifg.apurador.persistencia.adapter;

import br.edu.ifg.apurador.cluster.apuracao.DadosDaChave;
import br.edu.ifg.apurador.cluster.apuracao.EstadoDoEvento;
import br.edu.ifg.apurador.cluster.apuracao.EventoDeResultado;
import br.edu.ifg.apurador.cluster.apuracao.PartidaGravada;
import br.edu.ifg.apurador.cluster.despacho.ConsultaDePendencias;
import br.edu.ifg.apurador.cluster.despacho.Recibo;
import br.edu.ifg.apurador.cluster.despacho.RegistroDeEventos;
import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import br.edu.ifg.apurador.persistencia.domain.EventoEntidade;
import br.edu.ifg.apurador.persistencia.mapper.ChaveDadosMapper;
import br.edu.ifg.apurador.persistencia.mapper.EventoDeResultadoMapper;
import br.edu.ifg.apurador.persistencia.mapper.PartidaGravadaMapper;
import br.edu.ifg.apurador.persistencia.repository.ChaveRepository;
import br.edu.ifg.apurador.persistencia.repository.EventoRepository;
import br.edu.ifg.apurador.persistencia.repository.PartidaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistroJpa implements RegistroDeEventos, ConsultaDePendencias {

    private final ChaveRepository chaveRepository;
    private final PartidaRepository partidaRepository;
    private final EventoRepository eventoRepository;

    @Override
    public PartidaGravada partida(String partidaId) {
        return partidaId == null ? null : partidaRepository.findById(partidaId).map(PartidaGravadaMapper::toGravada).orElse(null);
    }

    @Override
    public DadosDaChave chave(String chaveId) {
        return chaveId == null ? null : chaveRepository.findById(chaveId).map(ChaveDadosMapper::toDados).orElse(null);
    }

    @Override
    public Recibo gravarPendente(EventoDeResultado evento) {
        Optional<EventoEntidade> existente = eventoRepository.findByEventoId(evento.getEventoId());
        if (existente.isPresent()) {
            return new Recibo(evento.getEventoId(), existente.get().getSeq(), false, null);
        }
        EventoEntidade novo = new EventoEntidade(evento.getEventoId(), evento.getChaveId(), evento.getPartidaId());
        EventoDeResultadoMapper.preencher(novo, evento);
        try {
            EventoEntidade gravado = eventoRepository.saveAndFlush(novo);
            return new Recibo(evento.getEventoId(), gravado.getSeq(), true, null);
        } catch (DataIntegrityViolationException repetido) {
            EventoEntidade primeiro = eventoRepository.findByEventoId(evento.getEventoId()).orElseThrow(() -> repetido);
            return new Recibo(evento.getEventoId(), primeiro.getSeq(), false, null);
        }
    }

    @Override
    public List<String> chavesConhecidas() {
        List<String> ids = new ArrayList<>();
        for (ChaveEntidade chave : chaveRepository.findAllByOrderByCriadaEmAscIdAsc()) {
            ids.add(chave.getId());
        }
        return ids;
    }

    @Override
    public List<String> chavesComPendentes(long idadeMinimaMs) {
        return eventoRepository.chavesComEstadoAte(EstadoDoEvento.PENDENTE, Instant.now().minusMillis(idadeMinimaMs));
    }
}
