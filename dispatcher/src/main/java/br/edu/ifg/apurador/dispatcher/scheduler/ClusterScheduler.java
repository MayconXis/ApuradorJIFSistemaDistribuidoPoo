package br.edu.ifg.apurador.dispatcher.scheduler;

import br.edu.ifg.apurador.cluster.despacho.Coordenador;
import br.edu.ifg.apurador.cluster.falhas.Transicao;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClusterScheduler {

    private final Coordenador coordenador;

    @Value("${apurador.varredura.idade-minima-ms:10000}")
    private long idadeMinimaMs;

    @Scheduled(fixedDelayString = "${apurador.heartbeat.verificacao-ms:1000}")
    public void verificarFalhas() {
        List<Transicao> transicoes = coordenador.verificarFalhas();
        for (Transicao transicao : transicoes) {
            log.warn("Detector de falhas: {}", transicao);
        }
    }

    @Scheduled(fixedDelayString = "${apurador.varredura.intervalo-ms:5000}")
    public void varrerPendentes() {
        int reenviadas = coordenador.varrerPendentes(idadeMinimaMs);
        if (reenviadas > 0) {
            log.info("Varredura: {} tarefa(s) reenviada(s)", reenviadas);
        }
    }
}
