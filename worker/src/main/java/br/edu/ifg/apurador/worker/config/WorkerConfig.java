package br.edu.ifg.apurador.worker.config;

import br.edu.ifg.apurador.cluster.apuracao.AplicadorDeEventos;
import br.edu.ifg.apurador.cluster.apuracao.HistoricoDaChave;
import br.edu.ifg.apurador.cluster.despacho.Filas;
import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerConfig {

    @Bean
    public CatalogoDeModalidades catalogoDeModalidades() {
        return CatalogoDeModalidades.padrao();
    }

    @Bean
    public AplicadorDeEventos aplicadorDeEventos(HistoricoDaChave historico, CatalogoDeModalidades catalogo,
                                                 @Value("${apurador.worker.id}") String workerId) {
        return new AplicadorDeEventos(historico, catalogo, workerId);
    }

    @Bean
    public DirectExchange exchangeDeTarefas() {
        return new DirectExchange(Filas.EXCHANGE_TAREFAS, true, false);
    }

    @Bean
    public FanoutExchange exchangeDeMortas() {
        return new FanoutExchange(Filas.EXCHANGE_MORTAS, true, false);
    }

    @Bean
    public Queue filaDoWorker(@Value("${apurador.worker.id}") String workerId,
                              @Value("${apurador.fila.limite-de-entregas:5}") int limiteDeEntregas) {
        return QueueBuilder.durable(Filas.filaDe(workerId))
                .quorum()
                .deliveryLimit(limiteDeEntregas)
                .deadLetterExchange(Filas.EXCHANGE_MORTAS)
                .build();
    }

    @Bean
    public Binding ligacaoDaFila(Queue filaDoWorker, DirectExchange exchangeDeTarefas,
                                 @Value("${apurador.worker.id}") String workerId) {
        return BindingBuilder.bind(filaDoWorker).to(exchangeDeTarefas).with(workerId);
    }
}
