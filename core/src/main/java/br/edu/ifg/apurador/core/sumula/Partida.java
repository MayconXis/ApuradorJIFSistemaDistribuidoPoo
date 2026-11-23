package br.edu.ifg.apurador.core.sumula;

/**
 * Um jogo entre duas equipes, com o que o regulamento usa para classificar:
 * placar, indicador de WO e ocorrências disciplinares. Imutável.
 *
 * O construtor é private. A partida nasce por um dos métodos de fábrica:
 * jogada(...) para o jogo disputado e porWO(...) para a vitória por ausência.
 * Uma partida por WO não guarda placar: ele é calculado pela modalidade
 * (veja Modalidade.placarDeWO) e anexado com comPlacar(...).
 */
public final class Partida {

    private final String id;
    private final Equipe mandante;
    private final Equipe visitante;
    private final Placar placar;
    private final boolean wo;
    private final Lado ausente;
    private final Disciplina disciplina;

    private Partida(String id, Equipe mandante, Equipe visitante, Placar placar,
                    boolean wo, Lado ausente, Disciplina disciplina) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("A partida precisa de um identificador.");
        }
        if (mandante == null || visitante == null || mandante.equals(visitante)) {
            throw new IllegalArgumentException("A partida precisa de duas equipes diferentes.");
        }
        this.id = id;
        this.mandante = mandante;
        this.visitante = visitante;
        this.placar = placar;
        this.wo = wo;
        this.ausente = ausente;
        this.disciplina = disciplina == null ? Disciplina.semOcorrencias() : disciplina;
    }

    public static Partida jogada(String id, Equipe mandante, Equipe visitante, Placar placar, Disciplina disciplina) {
        if (placar == null) {
            throw new IllegalArgumentException("Partida jogada precisa de placar.");
        }
        return new Partida(id, mandante, visitante, placar, false, null, disciplina);
    }

    /** Sobrecarga para a partida sem cartões nem sanções. */
    public static Partida jogada(String id, Equipe mandante, Equipe visitante, Placar placar) {
        return jogada(id, mandante, visitante, placar, Disciplina.semOcorrencias());
    }

    public static Partida porWO(String id, Equipe mandante, Equipe visitante, Lado ausente) {
        if (ausente == null) {
            throw new IllegalArgumentException("Informe qual lado não compareceu.");
        }
        return new Partida(id, mandante, visitante, null, true, ausente, Disciplina.semOcorrencias());
    }

    /** Cópia desta partida com o placar informado. Usada para anexar o placar do WO. */
    public Partida comPlacar(Placar placarEfetivo) {
        return new Partida(id, mandante, visitante, placarEfetivo, wo, ausente, disciplina);
    }

    public boolean envolve(Equipe equipe) {
        return mandante.equals(equipe) || visitante.equals(equipe);
    }

    public Lado ladoDe(Equipe equipe) {
        if (mandante.equals(equipe)) {
            return Lado.MANDANTE;
        }
        if (visitante.equals(equipe)) {
            return Lado.VISITANTE;
        }
        throw new IllegalArgumentException(equipe + " não jogou a partida " + id + ".");
    }

    public Equipe equipeDo(Lado lado) {
        return lado == Lado.MANDANTE ? mandante : visitante;
    }

    public String getId() {
        return id;
    }

    public Equipe getMandante() {
        return mandante;
    }

    public Equipe getVisitante() {
        return visitante;
    }

    public Placar getPlacar() {
        return placar;
    }

    public boolean temPlacar() {
        return placar != null;
    }

    public boolean isWo() {
        return wo;
    }

    public Lado getAusente() {
        return ausente;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }

    @Override
    public String toString() {
        String meio = wo ? " WO (ausente: " + equipeDo(ausente) + ") " : " " + placar + " ";
        return mandante + meio + visitante;
    }
}
