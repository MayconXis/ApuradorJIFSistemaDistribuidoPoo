package br.edu.ifg.apurador.dispatcher.config;

import br.edu.ifg.apurador.cluster.anel.AnelDeHashLimitado;
import br.edu.ifg.apurador.cluster.apuracao.CalculoDaTabela;
import br.edu.ifg.apurador.cluster.despacho.ConsultaDePendencias;
import br.edu.ifg.apurador.cluster.despacho.Coordenador;
import br.edu.ifg.apurador.cluster.despacho.PublicadorDeTarefas;
import br.edu.ifg.apurador.cluster.despacho.RegistradorDeResultados;
import br.edu.ifg.apurador.cluster.despacho.RegistroDeEventos;
import br.edu.ifg.apurador.cluster.falhas.DetectorDeFalhas;
import br.edu.ifg.apurador.cluster.falhas.Relogio;
import br.edu.ifg.apurador.cluster.falhas.RelogioDoSistema;
import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClusterConfig {

    @Bean
    public CatalogoDeModalidades catalogoDeModalidades() {
        return CatalogoDeModalidades.padrao();
    }

    @Bean
    public Relogio relogio() {
        return new RelogioDoSistema();
    }

    @Bean
    public AnelDeHashLimitado anel(@Value("${apurador.anel.pontos-por-worker:64}") int pontosPorWorker,
                                   @Value("${apurador.anel.folga:0.25}") double folga) {
        return new AnelDeHashLimitado(pontosPorWorker, folga);
    }

    @Bean
    public DetectorDeFalhas detectorDeFalhas(Relogio relogio,
                                             @Value("${apurador.heartbeat.limite-de-suspeita-ms:3000}") long limiteDeSuspeitaMs,
                                             @Value("${apurador.heartbeat.tempo-limite-ms:5000}") long tempoLimiteMs) {
        return new DetectorDeFalhas(relogio, limiteDeSuspeitaMs, tempoLimiteMs);
    }

    @Bean
    public Coordenador coordenador(AnelDeHashLimitado anel, DetectorDeFalhas detector,
                                   PublicadorDeTarefas publicador, ConsultaDePendencias pendencias) {
        return new Coordenador(anel, detector, publicador, pendencias);
    }

    @Bean
    public RegistradorDeResultados registradorDeResultados(RegistroDeEventos registro, Coordenador coordenador,
                                                           CatalogoDeModalidades catalogo) {
        return new RegistradorDeResultados(registro, coordenador, catalogo);
    }

    @Bean
    public CalculoDaTabela calculoDaTabela() {
        return new CalculoDaTabela();
    }
}
