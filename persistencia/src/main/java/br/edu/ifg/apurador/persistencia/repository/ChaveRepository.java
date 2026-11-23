package br.edu.ifg.apurador.persistencia.repository;

import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChaveRepository extends JpaRepository<ChaveEntidade, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChaveEntidade c where c.id = :id")
    Optional<ChaveEntidade> travar(@Param("id") String id);

    List<ChaveEntidade> findAllByOrderByCriadaEmAscIdAsc();
}
