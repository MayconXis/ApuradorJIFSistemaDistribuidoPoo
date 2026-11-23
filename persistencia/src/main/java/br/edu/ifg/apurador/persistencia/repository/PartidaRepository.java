package br.edu.ifg.apurador.persistencia.repository;

import br.edu.ifg.apurador.persistencia.domain.PartidaEntidade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartidaRepository extends JpaRepository<PartidaEntidade, String> {

    List<PartidaEntidade> findByChaveIdOrderByIdAsc(String chaveId);
}
