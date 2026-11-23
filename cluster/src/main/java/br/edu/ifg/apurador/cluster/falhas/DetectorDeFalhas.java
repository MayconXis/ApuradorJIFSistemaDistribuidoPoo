package br.edu.ifg.apurador.cluster.falhas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Algoritmo de tolerância a falhas do grupo, parte 1: detector por heartbeat com
 * tempo limite.
 *
 * Cada worker envia um heartbeat em intervalo fixo. O detector guarda o instante
 * do último heartbeat de cada um. Quando verificar() é chamado:
 * - silêncio maior que o limite de suspeita: o worker passa a SUSPEITO;
 * - silêncio maior que o tempo limite: o worker passa a MORTO.
 * Um heartbeat novo traz o worker de volta a VIVO.
 *
 * O detector só observa e avisa. Quem reage (tira do anel, reatribui as chaves)
 * é o Coordenador.
 *
 * Este detector pode errar: um worker lento ou com a rede atrasada pode ser
 * declarado morto sem ter caído (falso positivo). Tempo limite curto detecta
 * antes e erra mais. Tempo limite longo erra menos e demora a recuperar.
 * O sistema continua correto mesmo com o erro, porque a aplicação dos eventos
 * é idempotente e trava a chave no banco.
 */
public class DetectorDeFalhas {

    private final Relogio relogio;
    private final long limiteDeSuspeitaMs;
    private final long tempoLimiteMs;
    private final Map<String, Long> ultimoHeartbeat = new HashMap<>();
    private final Map<String, EstadoDoWorker> estados = new TreeMap<>();

    public DetectorDeFalhas(Relogio relogio, long limiteDeSuspeitaMs, long tempoLimiteMs) {
        if (limiteDeSuspeitaMs <= 0 || tempoLimiteMs < limiteDeSuspeitaMs) {
            throw new IllegalArgumentException("O tempo limite deve ser maior ou igual ao limite de suspeita.");
        }
        this.relogio = relogio;
        this.limiteDeSuspeitaMs = limiteDeSuspeitaMs;
        this.tempoLimiteMs = tempoLimiteMs;
    }

    /**
     * Registra um heartbeat.
     *
     * @return a transição para VIVO, se o worker é novo ou estava suspeito ou morto. Null se ele já estava vivo.
     */
    public synchronized Transicao registrarHeartbeat(String worker) {
        long agora = relogio.agora();
        ultimoHeartbeat.put(worker, agora);
        EstadoDoWorker anterior = estados.put(worker, EstadoDoWorker.VIVO);
        if (anterior == EstadoDoWorker.VIVO) {
            return null;
        }
        return new Transicao(worker, anterior, EstadoDoWorker.VIVO, agora);
    }

    /** Compara o silêncio de cada worker com os limites e devolve quem mudou de estado. */
    public synchronized List<Transicao> verificar() {
        long agora = relogio.agora();
        List<Transicao> mudancas = new ArrayList<>();
        for (Map.Entry<String, EstadoDoWorker> entrada : estados.entrySet()) {
            String worker = entrada.getKey();
            EstadoDoWorker atual = entrada.getValue();
            if (atual == EstadoDoWorker.MORTO) {
                continue;
            }
            long silencio = agora - ultimoHeartbeat.get(worker);
            EstadoDoWorker novo = EstadoDoWorker.VIVO;
            if (silencio > tempoLimiteMs) {
                novo = EstadoDoWorker.MORTO;
            } else if (silencio > limiteDeSuspeitaMs) {
                novo = EstadoDoWorker.SUSPEITO;
            }
            if (novo != atual && novo != EstadoDoWorker.VIVO) {
                entrada.setValue(novo);
                mudancas.add(new Transicao(worker, atual, novo, agora));
            }
        }
        return mudancas;
    }

    /** Estado do worker. Null se ele nunca mandou heartbeat. */
    public synchronized EstadoDoWorker estadoDe(String worker) {
        return estados.get(worker);
    }

    /** Milissegundos desde o último heartbeat do worker. */
    public synchronized long silencioDe(String worker) {
        Long ultimo = ultimoHeartbeat.get(worker);
        return ultimo == null ? Long.MAX_VALUE : relogio.agora() - ultimo;
    }

    public synchronized Map<String, EstadoDoWorker> estados() {
        return Collections.unmodifiableMap(new TreeMap<>(estados));
    }

    public long getLimiteDeSuspeitaMs() {
        return limiteDeSuspeitaMs;
    }

    public long getTempoLimiteMs() {
        return tempoLimiteMs;
    }
}
