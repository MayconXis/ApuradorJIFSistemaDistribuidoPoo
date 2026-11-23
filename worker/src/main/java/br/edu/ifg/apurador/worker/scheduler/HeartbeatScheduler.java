package br.edu.ifg.apurador.worker.scheduler;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class HeartbeatScheduler {

    private final RestClient restClient;
    private final String workerId;
    private volatile boolean pronto;
    private boolean avisouFalha;

    public HeartbeatScheduler(RestClient.Builder restClientBuilder,
                              @Value("${apurador.dispatcher.url}") String urlDoDispatcher,
                              @Value("${apurador.worker.id}") String workerId,
                              @Value("${apurador.heartbeat.espera-ms:800}") int esperaMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(esperaMs);
        fabrica.setReadTimeout(esperaMs);
        this.restClient = restClientBuilder.baseUrl(urlDoDispatcher).requestFactory(fabrica).build();
        this.workerId = workerId;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoFicarPronto() {
        pronto = true;
        log.info("Worker {} pronto. Heartbeats ligados.", workerId);
    }

    @Scheduled(fixedRateString = "${apurador.heartbeat.intervalo-ms:1000}")
    public void enviar() {
        if (!pronto) {
            return;
        }
        try {
            restClient.post().uri("/interno/heartbeats")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("workerId", workerId))
                    .retrieve()
                    .toBodilessEntity();
            if (avisouFalha) {
                log.info("Dispatcher voltou a responder aos heartbeats.");
                avisouFalha = false;
            }
        } catch (RuntimeException falha) {
            if (!avisouFalha) {
                log.warn("Heartbeat não entregue ao Dispatcher: {}", falha.toString());
                avisouFalha = true;
            }
        }
    }
}
