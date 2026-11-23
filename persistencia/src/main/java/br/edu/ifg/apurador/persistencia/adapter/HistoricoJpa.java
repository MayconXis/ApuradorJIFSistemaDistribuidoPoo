package br.edu.ifg.apurador.persistencia.adapter;

import br.edu.ifg.apurador.cluster.apuracao.AcaoNaChave;
import br.edu.ifg.apurador.cluster.apuracao.DadosDaChave;
import br.edu.ifg.apurador.cluster.apuracao.EstadoDaPartida;
import br.edu.ifg.apurador.cluster.apuracao.EstadoDoEvento;
import br.edu.ifg.apurador.cluster.apuracao.EventoDeResultado;
import br.edu.ifg.apurador.cluster.apuracao.HistoricoDaChave;
import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import br.edu.ifg.apurador.cluster.apuracao.PartidaGravada;
import br.edu.ifg.apurador.cluster.apuracao.TransacaoDaChave;
import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import br.edu.ifg.apurador.persistencia.domain.EventoEntidade;
import br.edu.ifg.apurador.persistencia.domain.PartidaEntidade;
import br.edu.ifg.apurador.persistencia.domain.PosicaoEntidade;
import br.edu.ifg.apurador.persistencia.mapper.ChaveDadosMapper;
import br.edu.ifg.apurador.persistencia.mapper.EventoDeResultadoMapper;
import br.edu.ifg.apurador.persistencia.mapper.PartidaGravadaMapper;
import br.edu.ifg.apurador.persistencia.repository.ChaveRepository;
import br.edu.ifg.apurador.persistencia.repository.EventoRepository;
import br.edu.ifg.apurador.persistencia.repository.PartidaRepository;
import br.edu.ifg.apurador.persistencia.repository.PosicaoRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HistoricoJpa implements HistoricoDaChave {

    private final ChaveRepository chaveRepository;
    private final PartidaRepository partidaRepository;
    private final EventoRepository eventoRepository;
    private final PosicaoRepository posicaoRepository;

    @Override
    @Transactional
    public <T> T comTrava(String chaveId, AcaoNaChave<T> acao) {
        ChaveEntidade chave = chaveRepository.travar(chaveId)
                .orElseThrow(() -> new IllegalArgumentException("Chave desconhecida: " + chaveId));
        return acao.executar(new TransacaoJpa(chave));
    }

    @RequiredArgsConstructor
    private final class TransacaoJpa implements TransacaoDaChave {

        private final ChaveEntidade chave;

        @Override
        public DadosDaChave chave() {
            return ChaveDadosMapper.toDados(chave);
        }

        @Override
        public List<EventoDeResultado> pendentes() {
            List<EventoDeResultado> lista = new ArrayList<>();
            for (EventoEntidade evento : eventoRepository.findByChaveIdAndEstadoOrderBySeqAsc(chave.getId(), EstadoDoEvento.PENDENTE)) {
                lista.add(EventoDeResultadoMapper.toEvento(evento));
            }
            return lista;
        }

        @Override
        public List<PartidaGravada> partidas() {
            List<PartidaGravada> lista = new ArrayList<>();
            for (PartidaEntidade partida : partidaRepository.findByChaveIdOrderByIdAsc(chave.getId())) {
                lista.add(PartidaGravadaMapper.toGravada(partida));
            }
            return lista;
        }

        @Override
        public void aplicar(EventoDeResultado evento, EstadoDaPartida novoEstado, String worker) {
            EventoEntidade linhaDoEvento = buscar(evento);
            PartidaEntidade partida = partidaRepository.findById(evento.getPartidaId())
                    .orElseThrow(() -> new IllegalStateException("Partida sumiu: " + evento.getPartidaId()));
            partida.receberResultado(linhaDoEvento, novoEstado);
            linhaDoEvento.marcarAplicado(worker);
            partidaRepository.save(partida);
            eventoRepository.save(linhaDoEvento);
        }

        @Override
        public void encerrar(EventoDeResultado evento, EstadoDoEvento estado, String motivo, String worker) {
            EventoEntidade linhaDoEvento = buscar(evento);
            linhaDoEvento.encerrar(estado, motivo, worker);
            eventoRepository.save(linhaDoEvento);
        }

        @Override
        public void gravarClassificacao(List<LinhaDaTabela> linhas) {
            for (LinhaDaTabela linha : linhas) {
                posicaoRepository.save(new PosicaoEntidade(chave.getId(), linha));
            }
            chave.avancarVersao();
            chaveRepository.save(chave);
        }

        private EventoEntidade buscar(EventoDeResultado evento) {
            return eventoRepository.findByEventoId(evento.getEventoId())
                    .orElseThrow(() -> new IllegalStateException("Evento sumiu: " + evento.getEventoId()));
        }
    }
}
