package br.edu.ifg.apurador.cluster.despacho;

/**
 * Mensagem que o Dispatcher publica na fila de um worker. Imutável.
 *
 * A tarefa não carrega o resultado: ela só avisa que a chave tem eventos pendentes
 * no PostgreSQL. Por isso uma mensagem repetida ou perdida não causa dano:
 * o worker sempre aplica o que estiver pendente para a chave.
 */
public final class Tarefa {

    /** Um resultado acabou de ser gravado. */
    public static final String NOVO_EVENTO = "NOVO_EVENTO";
    /** A chave mudou de dono porque um worker saiu ou entrou no anel. */
    public static final String REATRIBUICAO = "REATRIBUICAO";
    /** A varredura achou evento pendente há tempo demais. */
    public static final String VARREDURA = "VARREDURA";

    private final String chaveId;
    private final String motivo;

    public Tarefa(String chaveId, String motivo) {
        if (chaveId == null || chaveId.isEmpty()) {
            throw new IllegalArgumentException("A tarefa precisa da chave.");
        }
        this.chaveId = chaveId;
        this.motivo = motivo;
    }

    public String getChaveId() {
        return chaveId;
    }

    public String getMotivo() {
        return motivo;
    }

    @Override
    public String toString() {
        return "Tarefa{" + chaveId + ", " + motivo + "}";
    }
}
