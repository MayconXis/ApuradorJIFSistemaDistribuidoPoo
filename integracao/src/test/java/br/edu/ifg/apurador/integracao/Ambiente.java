package br.edu.ifg.apurador.integracao;

import br.edu.ifg.apurador.dispatcher.DispatcherApplication;
import br.edu.ifg.apurador.worker.WorkerApplication;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

final class Ambiente implements AutoCloseable {

    static final long TEMPO_LIMITE_MS = 1500;

    private PostgreSQLContainer<?> postgres;
    private RabbitMQContainer rabbit;
    final String dbUrl;
    final String dbUser;
    final String dbPassword;
    final String rabbitHost;
    final int rabbitPort;
    final String rabbitUser;
    final String rabbitPassword;

    Ambiente() {
        if ("1".equals(System.getenv("APURADOR_IT_EXTERNO"))) {
            dbUrl = env("DB_URL", "jdbc:postgresql://localhost:5432/apurador");
            dbUser = env("DB_USER", "apurador");
            dbPassword = env("DB_PASSWORD", "apurador");
            rabbitHost = env("RABBIT_HOST", "localhost");
            rabbitPort = Integer.parseInt(env("RABBIT_PORT", "5672"));
            rabbitUser = env("RABBIT_USER", "guest");
            rabbitPassword = env("RABBIT_PASSWORD", "guest");
        } else {
            postgres = new PostgreSQLContainer<>("postgres:16").withDatabaseName("apurador")
                    .withUsername("apurador").withPassword("apurador");
            rabbit = new RabbitMQContainer("rabbitmq:3.13-management");
            postgres.start();
            rabbit.start();
            dbUrl = postgres.getJdbcUrl();
            dbUser = postgres.getUsername();
            dbPassword = postgres.getPassword();
            rabbitHost = rabbit.getHost();
            rabbitPort = rabbit.getAmqpPort();
            rabbitUser = rabbit.getAdminUsername();
            rabbitPassword = rabbit.getAdminPassword();
        }
    }

    void prepararBanco() throws Exception {
        try (Connection conexao = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            ScriptUtils.executeSqlScript(conexao, new ClassPathResource("db/schema.sql"));
            try (Statement comando = conexao.createStatement()) {
                comando.execute("TRUNCATE posicao, partida, evento_resultado, chave_equipe, chave");
            }
        }
    }

    ConfigurableApplicationContext subirDispatcher() {
        Map<String, Object> p = comum("dispatcher");
        p.put("apurador.heartbeat.limite-de-suspeita-ms", "800");
        p.put("apurador.heartbeat.tempo-limite-ms", String.valueOf(TEMPO_LIMITE_MS));
        p.put("apurador.heartbeat.verificacao-ms", "200");
        p.put("apurador.varredura.intervalo-ms", "1000");
        p.put("apurador.varredura.idade-minima-ms", "2000");
        p.put("spring.rabbitmq.publisher-confirm-type", "correlated");
        p.put("spring.rabbitmq.publisher-returns", "true");
        p.put("spring.rabbitmq.template.mandatory", "true");
        return new SpringApplicationBuilder(DispatcherApplication.class).properties(p).run();
    }

    ConfigurableApplicationContext subirWorker(String workerId, int portaDoDispatcher) {
        Map<String, Object> p = comum(workerId);
        p.put("apurador.worker.id", workerId);
        p.put("apurador.dispatcher.url", "http://localhost:" + portaDoDispatcher);
        p.put("apurador.heartbeat.intervalo-ms", "200");
        p.put("apurador.fila.limite-de-entregas", "3");
        p.put("spring.rabbitmq.listener.simple.prefetch", "1");
        p.put("spring.rabbitmq.listener.simple.concurrency", "1");
        p.put("spring.rabbitmq.listener.simple.max-concurrency", "1");
        return new SpringApplicationBuilder(WorkerApplication.class).properties(p).run();
    }

    static int porta(ConfigurableApplicationContext contexto) {
        return Integer.parseInt(contexto.getEnvironment().getProperty("local.server.port"));
    }

    private Map<String, Object> comum(String nome) {
        Map<String, Object> p = new HashMap<>();
        p.put("spring.config.location", "optional:classpath:/integracao-sem-arquivo/");
        p.put("spring.application.name", nome);
        p.put("server.port", "0");
        p.put("spring.datasource.url", dbUrl);
        p.put("spring.datasource.username", dbUser);
        p.put("spring.datasource.password", dbPassword);
        p.put("spring.jpa.open-in-view", "false");
        p.put("spring.jpa.hibernate.ddl-auto", "validate");
        p.put("spring.rabbitmq.host", rabbitHost);
        p.put("spring.rabbitmq.port", String.valueOf(rabbitPort));
        p.put("spring.rabbitmq.username", rabbitUser);
        p.put("spring.rabbitmq.password", rabbitPassword);
        p.put("management.endpoint.health.probes.enabled", "true");
        return p;
    }

    private static String env(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    @Override
    public void close() {
        if (rabbit != null) {
            rabbit.stop();
        }
        if (postgres != null) {
            postgres.stop();
        }
    }
}
