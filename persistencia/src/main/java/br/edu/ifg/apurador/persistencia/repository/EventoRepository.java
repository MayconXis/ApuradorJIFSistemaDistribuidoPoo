package br.edu.ifg.apurador.persistencia.repository;

import br.edu.ifg.apurador.cluster.apuracao.EstadoDoEvento;
import br.edu.ifg.apurador.persistencia.domain.EventoEntidade;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRepository extends JpaRepository<EventoEntidade, Long> {

    Optional<EventoEntidade> findByEventoId(String eventoId);

    List<EventoEntidade> findByChaveIdAndEstadoOrderBySeqAsc(String chaveId, EstadoDoEvento estado);

    @Query("select distinct e.chaveId from EventoEntidade e where e.estado = :estado and e.recebidoEm <= :limite")
    List<String> chavesComEstadoAte(@Param("estado") EstadoDoEvento estado, @Param("limite") Instant limite);

    long countByEstado(EstadoDoEvento estado);

    @Query("select count(e) from EventoEntidade e where e.aplicacoes > 1")
    long contarAplicadosMaisDeUmaVez();
}
