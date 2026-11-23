package br.edu.ifg.apurador.cluster.despacho;

import br.edu.ifg.apurador.cluster.anel.AnelDeHashLimitado;
import br.edu.ifg.apurador.cluster.falhas.DetectorDeFalhas;
import br.edu.ifg.apurador.cluster.falhas.EstadoDoWorker;
import br.edu.ifg.apurador.cluster.falhas.Transicao;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O Dispatcher. Junta os dois algoritmos do grupo:
 * - balanceamento: pergunta ao anel quem é o dono da chave e publica a tarefa
 *   na fila desse worker;
 * - tolerância a falhas, parte 2: quando o detector declara um worker morto,
 *   tira o worker do anel, reatribui as chaves dele e reenvia as tarefas das
 *   chaves que ainda têm eventos pendentes.
 *
 * Esta classe é Java puro. O Spring só chama os métodos dela: o controlador REST
 * chama despachar e receberHeartbeat, e um agendador chama verificarFalhas e
 * varrerPendentes.
 *
 * Os métodos são synchronized porque são chamados por várias threads.
 */
public class Coordenador {

    private static final int MAXIMO_DE_MUDANCAS = 30;

    private final AnelDeHashLimitado anel;
    private final DetectorDeFalhas detector;
    private final PublicadorDeTarefas publicador;
    private final ConsultaDePendencias pendencias;
    private final Deque<String> mudancas = new ArrayDeque<>();

    public Coordenador(AnelDeHashLimitado anel, DetectorDeFalhas detector,
                       PublicadorDeTarefas publicador, ConsultaDePendencias pendencias) {
        this.anel = anel;
        this.detector = detector;
        this.publicador = publicador;
        this.pendencias = pendencias;
    }

    /**
     * Envia ao dono da chave o aviso de que ela tem eventos pendentes.
     *
     * @return o worker que recebeu a tarefa, ou null se não foi possível publicar
     *         (nenhum worker vivo ou falha no middleware). Nesse caso o evento
     *         continua pendente no banco e a varredura tenta de novo.
     */
    public synchronized String despachar(String chaveId, String motivo) {
        String dono = anel.donoDe(chaveId);
        if (dono == null) {
            return null;
        }
        try {
            publicador.publicar(dono, new Tarefa(chaveId, motivo));
            return dono;
        } catch (RuntimeException falha) {
            anotar("Falha ao publicar a chave " + chaveId + " para " + dono + ": " + falha.getMessage());
            return null;
        }
    }

    /**
     * Recebe o heartbeat de um worker. Se o worker é novo ou estava morto,
     * ele entra no anel e passa a receber chaves.
     */
    public synchronized void receberHeartbeat(String worker) {
        Transicao transicao = detector.registrarHeartbeat(worker);
        if (transicao == null || anel.workers().contains(worker)) {
            return;
        }
        Map<String, String> movidas = anel.adicionarWorker(worker);
        for (String chave : pendencias.chavesConhecidas()) {
            if (anel.donoAtual(chave) == null) {
                movidas.put(chave, anel.donoDe(chave));
            }
        }
        anotar(worker + (transicao.getDe() == null ? " entrou no anel." : " voltou ao anel.") + descrever(movidas));
        reenviarPendentes(movidas);
    }

    /**
     * Pergunta ao detector quem mudou de estado. Para cada worker declarado morto,
     * tira o worker do anel, reatribui as chaves dele e reenvia as pendentes.
     * Deve ser chamado em intervalo fixo.
     */
    public synchronized List<Transicao> verificarFalhas() {
        List<Transicao> transicoes = detector.verificar();
        for (Transicao transicao : transicoes) {
            if (transicao.getPara() == EstadoDoWorker.MORTO) {
                Map<String, String> movidas = anel.removerWorker(transicao.getWorker());
                anotar(transicao.getWorker() + " declarado MORTO." + descrever(movidas));
                reenviarPendentes(movidas);
            } else if (transicao.getPara() == EstadoDoWorker.SUSPEITO) {
                anotar(transicao.getWorker() + " está SUSPEITO: heartbeat atrasado.");
            }
        }
        return transicoes;
    }

    /**
     * Rede de segurança: reenvia a tarefa das chaves com evento pendente há mais
     * tempo que o informado. Cobre a mensagem que se perdeu, a publicação que
     * falhou e o período em que não havia worker vivo.
     *
     * @return quantas tarefas foram reenviadas
     */
    public synchronized int varrerPendentes(long idadeMinimaMs) {
        int reenviadas = 0;
        for (String chave : pendencias.chavesComPendentes(idadeMinimaMs)) {
            if (despachar(chave, Tarefa.VARREDURA) != null) {
                reenviadas++;
            }
        }
        return reenviadas;
    }

    /** Avisa o Dispatcher de que uma chave foi cadastrada, para ela já ganhar um dono. */
    public synchronized String registrarChave(String chaveId) {
        return anel.donoDe(chaveId);
    }

    /** Retrato do cluster para o painel. */
    public synchronized VisaoDoCluster visao() {
        List<VisaoDoWorker> workers = new ArrayList<>();
        for (Map.Entry<String, EstadoDoWorker> entrada : detector.estados().entrySet()) {
            String worker = entrada.getKey();
            workers.add(new VisaoDoWorker(worker, entrada.getValue(), detector.silencioDe(worker), anel.chavesDe(worker)));
        }
        return new VisaoDoCluster(workers, anel.teto(), anel.totalDeChaves(), anel.pontosPorWorker(), anel.folga(),
                detector.getTempoLimiteMs(), new ArrayList<>(mudancas));
    }

    private void reenviarPendentes(Map<String, String> movidas) {
        if (movidas.isEmpty()) {
            return;
        }
        Set<String> comPendentes = new HashSet<>(pendencias.chavesComPendentes(0));
        for (String chave : movidas.keySet()) {
            if (comPendentes.contains(chave)) {
                despachar(chave, Tarefa.REATRIBUICAO);
            }
        }
    }

    private String descrever(Map<String, String> movidas) {
        if (movidas.isEmpty()) {
            return "";
        }
        StringBuilder texto = new StringBuilder(" Chaves reatribuídas:");
        for (Map.Entry<String, String> entrada : movidas.entrySet()) {
            texto.append(" ").append(entrada.getKey()).append(" para ")
                    .append(entrada.getValue() == null ? "ninguém (sem worker vivo)" : entrada.getValue()).append(";");
        }
        return texto.toString();
    }

    private void anotar(String texto) {
        mudancas.addFirst(texto);
        while (mudancas.size() > MAXIMO_DE_MUDANCAS) {
            mudancas.removeLast();
        }
    }
}
