package br.edu.ifg.apurador.cluster.anel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Algoritmo de balanceamento do grupo: hash consistente com carga limitada.
 *
 * Ideia: workers e chaves ocupam posições em um anel de números. Cada worker
 * aparece em vários pontos (pontos virtuais), para a divisão ficar mais uniforme.
 * A chave vai ao primeiro worker que aparece adiante da posição dela. Se esse
 * worker já está no teto de carga, a chave pula para o seguinte.
 *
 * Teto de carga: teto = arredonda para cima ((1 + folga) x chaves / workers).
 * Com folga de 0,25, nenhum worker passa de 25% acima da média.
 *
 * Por que hash consistente e não round-robin: a mesma chave cai sempre no mesmo
 * worker, então os eventos dela são processados em ordem por um único processo.
 * Quando um worker sai, só as chaves dele mudam de dono.
 *
 * A técnica vem de Mirrokni, Thorup e Zadimoghaddam, "Consistent Hashing with
 * Bounded Loads" (SODA 2018). A implementação é nossa e não usa o plugin de hash
 * consistente do RabbitMQ nem o Spring Cloud LoadBalancer.
 *
 * A carga de um worker é a quantidade de chaves atribuídas a ele.
 * Todos os métodos públicos são synchronized: o anel é consultado pelas
 * requisições HTTP e alterado pelo detector de falhas ao mesmo tempo.
 */
public class AnelDeHashLimitado {

    private final int pontosPorWorker;
    private final double folga;

    /** Posição no anel -> worker dono daquele ponto virtual. */
    private final TreeMap<Long, String> anel = new TreeMap<>();
    private final Set<String> workers = new TreeSet<>();
    /** Chave -> worker dono, na ordem em que as chaves chegaram. */
    private final Map<String, String> donos = new LinkedHashMap<>();
    /** Chaves que chegaram quando não havia nenhum worker vivo. */
    private final Set<String> semDono = new LinkedHashSet<>();
    private final Map<String, Integer> cargas = new HashMap<>();

    public AnelDeHashLimitado(int pontosPorWorker, double folga) {
        if (pontosPorWorker < 1) {
            throw new IllegalArgumentException("Cada worker precisa de pelo menos um ponto no anel.");
        }
        if (folga < 0) {
            throw new IllegalArgumentException("A folga não pode ser negativa.");
        }
        this.pontosPorWorker = pontosPorWorker;
        this.folga = folga;
    }

    /**
     * Dono da chave. Se a chave ainda não tem dono, escolhe um agora.
     * Devolve null quando não há nenhum worker no anel: a chave fica aguardando.
     */
    public synchronized String donoDe(String chave) {
        String dono = donos.get(chave);
        if (dono != null) {
            return dono;
        }
        if (workers.isEmpty()) {
            semDono.add(chave);
            return null;
        }
        semDono.remove(chave);
        int teto = tetoPara(donos.size() + 1, workers.size());
        dono = procurar(chave, teto, null);
        atribuir(chave, dono);
        return dono;
    }

    /** Dono atual da chave, sem atribuir. Null se ela não tem dono. */
    public synchronized String donoAtual(String chave) {
        return donos.get(chave);
    }

    /**
     * Inclui um worker no anel.
     *
     * 1. O worker novo recebe as chaves que agora caem primeiro nele, até o teto.
     * 2. Como a média por worker diminuiu, o teto diminuiu. Quem ficou acima do
     *    teto novo passa adiante as chaves que recebeu por último.
     * 3. As chaves que estavam sem dono são atribuídas.
     *
     * @return as chaves que mudaram de dono, com o dono novo
     */
    public synchronized Map<String, String> adicionarWorker(String worker) {
        Map<String, String> movidas = new LinkedHashMap<>();
        if (!workers.add(worker)) {
            return movidas;
        }
        for (int i = 0; i < pontosPorWorker; i++) {
            anel.put(hash(worker + "#" + i), worker);
        }
        cargas.put(worker, 0);
        int teto = tetoPara(donos.size() + semDono.size(), workers.size());

        for (String chave : new ArrayList<>(donos.keySet())) {
            if (cargaDe(worker) >= teto) {
                break;
            }
            if (!worker.equals(donos.get(chave)) && worker.equals(primeiroAdiante(chave))) {
                atribuir(chave, worker);
                movidas.put(chave, worker);
            }
        }
        for (String sobrecarregado : new ArrayList<>(workers)) {
            List<String> dele = chavesDe(sobrecarregado);
            for (int i = dele.size() - 1; i >= 0 && cargaDe(sobrecarregado) > teto; i--) {
                String chave = dele.get(i);
                String novoDono = procurar(chave, teto, sobrecarregado);
                atribuir(chave, novoDono);
                movidas.put(chave, novoDono);
            }
        }
        for (String chave : new ArrayList<>(semDono)) {
            String dono = procurar(chave, teto, null);
            semDono.remove(chave);
            atribuir(chave, dono);
            movidas.put(chave, dono);
        }
        return movidas;
    }

    /**
     * Retira um worker do anel e entrega as chaves dele aos próximos workers,
     * respeitando o teto. As chaves dos outros workers não mudam de dono.
     *
     * @return as chaves que eram do worker, com o dono novo (null se não sobrou worker)
     */
    public synchronized Map<String, String> removerWorker(String worker) {
        Map<String, String> movidas = new LinkedHashMap<>();
        if (!workers.remove(worker)) {
            return movidas;
        }
        anel.values().removeIf(worker::equals);
        List<String> orfas = chavesDe(worker);
        cargas.remove(worker);
        for (String chave : orfas) {
            donos.remove(chave);
        }
        int teto = workers.isEmpty() ? 0 : tetoPara(donos.size() + orfas.size(), workers.size());
        for (String chave : orfas) {
            if (workers.isEmpty()) {
                semDono.add(chave);
                movidas.put(chave, null);
            } else {
                String novoDono = procurar(chave, teto, null);
                atribuir(chave, novoDono);
                movidas.put(chave, novoDono);
            }
        }
        return movidas;
    }

    /** Teto de carga para a quantidade atual de chaves e de workers. */
    public synchronized int teto() {
        return workers.isEmpty() ? 0 : tetoPara(donos.size(), workers.size());
    }

    public synchronized int cargaDe(String worker) {
        Integer carga = cargas.get(worker);
        return carga == null ? 0 : carga;
    }

    /** Chaves do worker, na ordem em que chegaram. */
    public synchronized List<String> chavesDe(String worker) {
        List<String> chaves = new ArrayList<>();
        for (Map.Entry<String, String> entrada : donos.entrySet()) {
            if (entrada.getValue().equals(worker)) {
                chaves.add(entrada.getKey());
            }
        }
        return chaves;
    }

    public synchronized Set<String> workers() {
        return Collections.unmodifiableSet(new TreeSet<>(workers));
    }

    public synchronized Map<String, String> atribuicoes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(donos));
    }

    public synchronized int totalDeChaves() {
        return donos.size() + semDono.size();
    }

    public int pontosPorWorker() {
        return pontosPorWorker;
    }

    public double folga() {
        return folga;
    }

    /** teto = arredonda para cima ((1 + folga) x chaves / workers), no mínimo 1. */
    int tetoPara(int chaves, int qtdWorkers) {
        return Math.max(1, (int) Math.ceil((1.0 + folga) * chaves / qtdWorkers));
    }

    /** Primeiro worker adiante da chave no anel, sem olhar a carga. */
    private String primeiroAdiante(String chave) {
        Map.Entry<Long, String> ponto = anel.ceilingEntry(hash(chave));
        return ponto != null ? ponto.getValue() : anel.firstEntry().getValue();
    }

    /**
     * Anda no anel a partir da posição da chave e devolve o primeiro worker com
     * carga abaixo do teto. Sempre existe um: o teto nunca é menor que a média.
     */
    private String procurar(String chave, int teto, String evitar) {
        long posicao = hash(chave);
        Set<String> vistos = new LinkedHashSet<>();
        List<String> volta = new ArrayList<>(anel.tailMap(posicao, true).values());
        volta.addAll(anel.headMap(posicao, false).values());
        for (String worker : volta) {
            if (!vistos.add(worker) || worker.equals(evitar)) {
                continue;
            }
            if (cargaDe(worker) < teto) {
                return worker;
            }
        }
        throw new IllegalStateException("Nenhum worker abaixo do teto " + teto + " para a chave " + chave);
    }

    private void atribuir(String chave, String worker) {
        String anterior = donos.put(chave, worker);
        if (anterior != null) {
            cargas.put(anterior, cargaDe(anterior) - 1);
        }
        cargas.put(worker, cargaDe(worker) + 1);
    }

    /** Posição no anel: os 8 primeiros bytes do MD5 do texto. O MD5 serve aqui só para espalhar bem. */
    static long hash(String texto) {
        try {
            byte[] resumo = MessageDigest.getInstance("MD5").digest(texto.getBytes(StandardCharsets.UTF_8));
            long valor = 0;
            for (int i = 0; i < 8; i++) {
                valor = (valor << 8) | (resumo[i] & 0xFF);
            }
            return valor;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 indisponível nesta JVM.", e);
        }
    }
}
