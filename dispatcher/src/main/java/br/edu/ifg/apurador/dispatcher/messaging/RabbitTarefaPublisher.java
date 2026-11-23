package br.edu.ifg.apurador.dispatcher.messaging;

import br.edu.ifg.apurador.cluster.despacho.Filas;
import br.edu.ifg.apurador.cluster.despacho.PublicadorDeTarefas;
import br.edu.ifg.apurador.cluster.despacho.Tarefa;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitTarefaPublisher implements PublicadorDeTarefas {

    private final RabbitTemplate rabbitTemplate;

    @Value("${apurador.publicacao.espera-ms:2000}")
    private long esperaMs;

    @Override
    public void publicar(String worker, Tarefa tarefa) {
        Message mensagem = MessageBuilder.withBody(tarefa.getChaveId().getBytes(StandardCharsets.UTF_8))
                .setContentType("text/plain")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader(Filas.CABECALHO_MOTIVO, tarefa.getMotivo())
                .build();
        CorrelationData correlacao = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.send(Filas.EXCHANGE_TAREFAS, worker, mensagem, correlacao);
        try {
            CorrelationData.Confirm confirmacao = correlacao.getFuture().get(esperaMs, TimeUnit.MILLISECONDS);
            if (!confirmacao.isAck()) {
                throw new IllegalStateException("o RabbitMQ recusou a tarefa: " + confirmacao.getReason());
            }
            if (correlacao.getReturned() != null) {
                throw new IllegalStateException("nenhuma fila recebeu a tarefa de " + worker);
            }
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("publicação interrompida", interrompido);
        } catch (ExecutionException | TimeoutException semResposta) {
            throw new IllegalStateException("sem confirmação do RabbitMQ em " + esperaMs + " ms", semResposta);
        }
    }
}
