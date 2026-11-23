package br.edu.ifg.apurador.persistencia.repository;

import br.edu.ifg.apurador.persistencia.domain.PosicaoEntidade;
import br.edu.ifg.apurador.persistencia.domain.PosicaoId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosicaoRepository extends JpaRepository<PosicaoEntidade, PosicaoId> {

    List<PosicaoEntidade> findByChaveIdOrderByColocacaoAsc(String chaveId);
}
