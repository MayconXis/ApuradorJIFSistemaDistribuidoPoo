package br.edu.ifg.apurador.persistencia.config;

import br.edu.ifg.apurador.persistencia.adapter.HistoricoJpa;
import br.edu.ifg.apurador.persistencia.domain.ChaveEntidade;
import br.edu.ifg.apurador.persistencia.repository.ChaveRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackageClasses = ChaveEntidade.class)
@EnableJpaRepositories(basePackageClasses = ChaveRepository.class)
@ComponentScan(basePackageClasses = HistoricoJpa.class)
public class PersistenciaConfig {
}
