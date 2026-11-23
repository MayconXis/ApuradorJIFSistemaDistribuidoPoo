package br.edu.ifg.apurador.cluster.apoio;

import br.edu.ifg.apurador.cluster.despacho.PublicadorDeTarefas;
import br.edu.ifg.apurador.cluster.despacho.Tarefa;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Middleware de mentira para os testes: uma fila por worker, com confirmação manual.
 * Imita o que o RabbitMQ faz: a mensagem entregue e não confirmada volta para a
 * fila quando o consumidor cai.
 */
public final class FilaEmMemoria implements PublicadorDeTarefas {

    private final Map<String, Deque<Tarefa>> filas = new HashMap<>();
    private final Map<String, List<Tarefa>> naoConfirmadas = new HashMap<>();
    private final List<String> publicacoes = new ArrayList<>();
    private int falhasProgramadas;

    @Override
    public synchronized void publicar(String worker, Tarefa tarefa) {
        if (falhasProgramadas > 0) {
            falhasProgramadas--;
            throw new IllegalStateException("middleware fora do ar (falha simulada)");
        }
        fila(worker).addLast(tarefa);
        publicacoes.add(worker + ":" + tarefa.getChaveId() + ":" + tarefa.getMotivo());
    }

    /** Entrega a próxima tarefa ao worker. Ela fica como não confirmada até o ack. Null se a fila está vazia. */
    public synchronized Tarefa receber(String worker) {
        Tarefa tarefa = fila(worker).pollFirst();
        if (tarefa != null) {
            naoConfirmadas.computeIfAbsent(worker, w -> new ArrayList<>()).add(tarefa);
        }
        return tarefa;
    }

    public synchronized void confirmar(String worker, Tarefa tarefa) {
        naoConfirmadas.computeIfAbsent(worker, w -> new ArrayList<>()).remove(tarefa);
    }

    /** O consumidor caiu: o que foi entregue e não confirmado volta para o começo da fila. */
    public synchronized int devolverNaoConfirmadas(String worker) {
        List<Tarefa> pendentes = naoConfirmadas.computeIfAbsent(worker, w -> new ArrayList<>());
        for (int i = pendentes.size() - 1; i >= 0; i--) {
            fila(worker).addFirst(pendentes.get(i));
        }
        int devolvidas = pendentes.size();
        pendentes.clear();
        return devolvidas;
    }

    public synchronized int tamanho(String worker) {
        return fila(worker).size();
    }

    public synchronized List<String> publicacoes() {
        return new ArrayList<>(publicacoes);
    }

    public synchronized void falharNasProximas(int quantidade) {
        falhasProgramadas = quantidade;
    }

    private Deque<Tarefa> fila(String worker) {
        return filas.computeIfAbsent(worker, w -> new ArrayDeque<>());
    }
}
