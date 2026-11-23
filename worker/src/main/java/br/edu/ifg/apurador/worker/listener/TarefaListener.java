package br.edu.ifg.apurador.worker.listener;

import br.edu.ifg.apurador.cluster.apuracao.AplicadorDeEventos;
import br.edu.ifg.apurador.cluster.apuracao.ResumoDaApuracao;
import br.edu.ifg.apurador.cluster.despacho.Filas;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TarefaListener {

    private final AplicadorDeEventos aplicadorDeEventos;

    @Value("${apurador.worker.atraso-ms:0}")
    private long atrasoMs;

    @RabbitListener(queues = "#{filaDoWorker.name}", ackMode = "MANUAL")
    public void receber(Message mensagem, Channel canal) throws IOException {
        long etiqueta = mensagem.getMessageProperties().getDeliveryTag();
        String chaveId = new String(mensagem.getBody(), StandardCharsets.UTF_8);
        Object motivo = mensagem.getMessageProperties().getHeaders().get(Filas.CABECALHO_MOTIVO);
        try {
            esperarSeConfigurado();
            ResumoDaApuracao resumo = aplicadorDeEventos.processarChave(chaveId);
            canal.basicAck(etiqueta, false);
            log.info("chave={} motivo={} aplicados={} rejeitados={} superados={}", chaveId, motivo,
                    resumo.getAplicados(), resumo.getRejeitados(), resumo.getSuperados());
        } catch (RuntimeException falha) {
            log.warn("Falha ao processar a chave {}: {}", chaveId, falha.toString());
            canal.basicNack(etiqueta, false, true);
        }
    }

    private void esperarSeConfigurado() {
        if (atrasoMs <= 0) {
            return;
        }
        try {
            Thread.sleep(atrasoMs);
        } catch (InterruptedException interrupcao) {
            Thread.currentThread().interrupt();
        }
    }
}
