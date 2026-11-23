package br.edu.ifg.apurador.dispatcher.config;

import br.edu.ifg.apurador.cluster.despacho.Filas;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange exchangeDeTarefas() {
        return new DirectExchange(Filas.EXCHANGE_TAREFAS, true, false);
    }

    @Bean
    public FanoutExchange exchangeDeMortas() {
        return new FanoutExchange(Filas.EXCHANGE_MORTAS, true, false);
    }

    @Bean
    public Queue filaDeMortas() {
        return QueueBuilder.durable(Filas.FILA_MORTAS).quorum().build();
    }

    @Bean
    public Binding ligacaoDasMortas(Queue filaDeMortas, FanoutExchange exchangeDeMortas) {
        return BindingBuilder.bind(filaDeMortas).to(exchangeDeMortas);
    }
}
