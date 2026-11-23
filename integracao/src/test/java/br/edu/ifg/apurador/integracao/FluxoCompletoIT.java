package br.edu.ifg.apurador.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.ifg.apurador.cluster.despacho.Filas;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ConfigurableApplicationContext;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FluxoCompletoIT {

    private static final Duration ESPERA = Duration.ofSeconds(30);
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private final List<String> relatorio = new ArrayList<>();
    private final String sufixo = UUID.randomUUID().toString().substring(0, 6);
    private final String workerA = "it-a-" + sufixo;
    private final String workerB = "it-b-" + sufixo;

    private Ambiente ambiente;
    private ConfigurableApplicationContext dispatcher;
    private ConfigurableApplicationContext contextoA;
    private ConfigurableApplicationContext contextoB;
    private String base;

    @BeforeAll
    void subir() throws Exception {
        ambiente = new Ambiente();
        ambiente.prepararBanco();
        dispatcher = ambiente.subirDispatcher();
        int porta = Ambiente.porta(dispatcher);
        base = "http://localhost:" + porta;
        contextoA = ambiente.subirWorker(workerA, porta);
        contextoB = ambiente.subirWorker(workerB, porta);
        esperar("dois workers vivos", () -> workersVivos() == 2);
    }

    @AfterAll
    void descer() throws IOException {
        for (ConfigurableApplicationContext contexto : new ConfigurableApplicationContext[] {contextoA, contextoB, dispatcher}) {
            if (contexto != null && contexto.isActive()) {
                contexto.close();
            }
        }
        if (ambiente != null) {
            ambiente.close();
        }
        Path saida = Path.of("target", "ti-02.txt");
        Files.createDirectories(saida.getParent());
        Files.write(saida, relatorio, StandardCharsets.UTF_8);
        relatorio.forEach(System.out::println);
    }

    @Test
    @Order(1)
    void empateTriploNoFutsalPassaPeloSistemaInteiro() throws Exception {
        JsonNode chave = post("/api/chaves", "{\"id\":\"it-triplo\",\"nome\":\"Chave A\",\"modalidade\":\"FUTSAL\","
                + "\"equipes\":[\"IFCE\",\"IFG\",\"IFSP\"]}", 201);
        assertThat(chave.get("equipes")).hasSize(3);
        JsonNode partidas = get("/api/chaves/it-triplo/partidas", 200);
        assertThat(partidas).hasSize(3);

        long inicio = System.nanoTime();
        registrar("t1", "it-triplo-J1", 4, 1);
        registrar("t2", "it-triplo-J3", 3, 1);
        registrar("t3", "it-triplo-J2", 3, 4);
        esperar("três eventos aplicados", () -> estado("t1").equals("APLICADO") && estado("t2").equals("APLICADO")
                && estado("t3").equals("APLICADO"));
        relatorio.add(String.format("TI-02 empate triplo: 3 resultados aplicados em %d ms (do primeiro POST ao último APLICADO)",
                (System.nanoTime() - inicio) / 1_000_000));

        JsonNode tabela = get("/api/chaves/it-triplo/classificacao", 200).get("posicoes");
        assertThat(tabela).hasSize(3);
        assertThat(tabela.get(0).get("equipe").asText()).isEqualTo("IFCE");
        assertThat(tabela.get(1).get("equipe").asText()).isEqualTo("IFG");
        assertThat(tabela.get(2).get("equipe").asText()).isEqualTo("IFSP");
        assertThat(tabela.get(0).get("criterioDecisivo").asText()).isEqualTo("Saldo de gols");
        assertThat(tabela.get(1).get("criterioDecisivo").asText()).isEqualTo("Confronto direto");
        assertThat(get("/api/chaves/it-triplo/auditoria", 200).get("consistente").asBoolean()).isTrue();
    }

    @Test
    @Order(2)
    void reenvioDoMesmoEventoNaoDuplica() throws Exception {
        JsonNode recibo = registrar("t1", "it-triplo-J1", 4, 1);
        assertThat(recibo.get("novo").asBoolean()).isFalse();
        Thread.sleep(500);
        JsonNode k = get("/api/consistencia", 200);
        assertThat(k.get("aplicadosMaisDeUmaVez").asLong()).isZero();
        assertThat(get("/api/chaves/it-triplo/auditoria", 200).get("consistente").asBoolean()).isTrue();
    }

    @Test
    @Order(3)
    void resultadoInvalidoVoltaCom422() throws Exception {
        String corpo = "{\"eventoId\":\"invalido-" + sufixo + "\",\"partidaId\":\"it-triplo-J1\","
                + "\"placarMandante\":-1,\"placarVisitante\":2}";
        HttpResponse<String> r = enviar("POST", "/api/resultados", corpo);
        assertThat(r.statusCode()).isEqualTo(422);
    }

    @Test
    @Order(4)
    void retificacaoRecalculaATabela() throws Exception {
        registrar("t4", "it-triplo-J2", 4, 4);
        esperar("retificação aplicada", () -> estado("t4").equals("APLICADO"));
        JsonNode tabela = get("/api/chaves/it-triplo/classificacao", 200).get("posicoes");
        assertThat(tabela.get(0).get("equipe").asText()).isEqualTo("IFCE");
        assertThat(tabela.get(0).get("pontos").asInt()).isEqualTo(4);
        assertThat(get("/api/chaves/it-triplo/auditoria", 200).get("consistente").asBoolean()).isTrue();
    }

    @Test
    @Order(5)
    void mensagemQueSempreFalhaVaiParaADlq() throws Exception {
        AmqpAdmin admin = dispatcher.getBean(AmqpAdmin.class);
        long antes = mensagensNaDlq(admin);
        RabbitTemplate rabbit = dispatcher.getBean(RabbitTemplate.class);
        Message veneno = new Message("chave-que-nao-existe".getBytes(StandardCharsets.UTF_8), new MessageProperties());
        rabbit.send(Filas.EXCHANGE_TAREFAS, workerB, veneno);
        esperar("mensagem na DLQ", () -> mensagensNaDlq(admin) == antes + 1);
        relatorio.add("TI-02 DLQ: a mensagem com chave inexistente foi para a " + Filas.FILA_MORTAS
                + " depois do limite de 3 devoluções");
    }

    @Test
    @Order(6)
    void quedaDeUmWorkerReatribuiAsChavesDele() throws Exception {
        for (int i = 1; i <= 6; i++) {
            post("/api/chaves", "{\"id\":\"it-k" + i + "\",\"nome\":\"Chave " + i + "\",\"modalidade\":\"FUTSAL\","
                    + "\"equipes\":[\"A" + i + "\",\"B" + i + "\",\"C" + i + "\",\"D" + i + "\"]}", 201);
        }
        List<String> chavesDeA = chavesDo(workerA);
        assertThat(chavesDeA).as("o worker A precisa ter alguma chave para o teste fazer sentido").isNotEmpty();
        relatorio.add("TI-02 antes da queda: " + workerA + " com " + chavesDeA.size() + " chaves, "
                + workerB + " com " + chavesDo(workerB).size());

        long parou = System.nanoTime();
        contextoA.close();
        esperar("worker A declarado morto e sem chaves", () -> "MORTO".equals(estadoDoWorker(workerA))
                && chavesDo(workerA).isEmpty());
        long ms = (System.nanoTime() - parou) / 1_000_000;
        relatorio.add(String.format("TI-02 queda do worker A: %d ms até as %d chaves dele terem novo dono "
                + "(tempo limite do detector configurado em %d ms)", ms, chavesDeA.size(), Ambiente.TEMPO_LIMITE_MS));

        String chave = chavesDeA.get(0);
        registrar("f1", chave + "-J1", 2, 0);
        registrar("f2", chave + "-J2", 1, 1);
        esperar("eventos aplicados depois da queda", () -> estado("f1").equals("APLICADO") && estado("f2").equals("APLICADO"));
        assertThat(get("/api/eventos/f1-" + sufixo, 200).get("worker").asText()).isEqualTo(workerB);
        assertThat(get("/api/chaves/" + chave + "/auditoria", 200).get("consistente").asBoolean()).isTrue();
    }

    @Test
    @Order(7)
    void nadaPerdidoNemDuplicadoNoFim() throws Exception {
        esperar("nenhum pendente", () -> consistencia().get("pendentes").asLong() == 0);
        JsonNode k = consistencia();
        relatorio.add("TI-02 consistência final: " + k);
        assertThat(k.get("aplicadosMaisDeUmaVez").asLong()).isZero();
        assertThat(k.get("chavesInconsistentes").asLong()).isZero();
    }

    private JsonNode registrar(String rotulo, String partidaId, int mandante, int visitante) throws Exception {
        String corpo = String.format("{\"eventoId\":\"%s-%s\",\"partidaId\":\"%s\",\"placarMandante\":%d,\"placarVisitante\":%d}",
                rotulo, sufixo, partidaId, mandante, visitante);
        return post("/api/resultados", corpo, 202);
    }

    private String estado(String rotulo) {
        try {
            HttpResponse<String> r = enviar("GET", "/api/eventos/" + rotulo + "-" + sufixo, null);
            return r.statusCode() == 200 ? json.readTree(r.body()).get("estado").asText() : "AUSENTE";
        } catch (Exception e) {
            return "ERRO";
        }
    }

    private long workersVivos() {
        try {
            long vivos = 0;
            for (JsonNode w : get("/api/cluster", 200).get("workers")) {
                if ("VIVO".equals(w.get("estado").asText())) {
                    vivos++;
                }
            }
            return vivos;
        } catch (Exception e) {
            return -1;
        }
    }

    private String estadoDoWorker(String id) {
        try {
            for (JsonNode w : get("/api/cluster", 200).get("workers")) {
                if (id.equals(w.get("id").asText())) {
                    return w.get("estado").asText();
                }
            }
            return "AUSENTE";
        } catch (Exception e) {
            return "ERRO";
        }
    }

    private List<String> chavesDo(String id) {
        List<String> chaves = new ArrayList<>();
        try {
            for (JsonNode w : get("/api/cluster", 200).get("workers")) {
                if (id.equals(w.get("id").asText())) {
                    w.get("chaves").forEach(c -> chaves.add(c.asText()));
                }
            }
        } catch (Exception e) {
            chaves.add("ERRO");
        }
        return chaves;
    }

    private JsonNode consistencia() {
        try {
            return get("/api/consistencia", 200);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static long mensagensNaDlq(AmqpAdmin admin) {
        QueueInformation info = admin.getQueueInfo(Filas.FILA_MORTAS);
        return info == null ? 0 : info.getMessageCount();
    }

    private JsonNode get(String caminho, int esperado) throws Exception {
        HttpResponse<String> r = enviar("GET", caminho, null);
        assertThat(r.statusCode()).as("GET " + caminho + ": " + r.body()).isEqualTo(esperado);
        return json.readTree(r.body());
    }

    private JsonNode post(String caminho, String corpo, int esperado) throws Exception {
        HttpResponse<String> r = enviar("POST", caminho, corpo);
        assertThat(r.statusCode()).as("POST " + caminho + ": " + r.body()).isEqualTo(esperado);
        return json.readTree(r.body());
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String corpo) throws Exception {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(base + caminho)).timeout(Duration.ofSeconds(10));
        if (corpo == null) {
            pedido.GET();
        } else {
            pedido.header("Content-Type", "application/json").method(metodo, HttpRequest.BodyPublishers.ofString(corpo));
        }
        return http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void esperar(String oQue, BooleanSupplier condicao) throws InterruptedException {
        long limite = System.nanoTime() + ESPERA.toNanos();
        while (System.nanoTime() < limite) {
            if (condicao.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Tempo esgotado esperando: " + oQue);
    }
}
