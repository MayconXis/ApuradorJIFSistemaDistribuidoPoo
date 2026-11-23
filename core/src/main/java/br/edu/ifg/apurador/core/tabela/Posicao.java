package br.edu.ifg.apurador.core.tabela;

import br.edu.ifg.apurador.core.sumula.Equipe;
import java.util.Objects;

/**
 * Uma linha da classificação final: a colocação da equipe e o critério que a decidiu.
 * Imutável: todos os atributos são final e não há setters. Dois cálculos com os
 * mesmos jogos produzem posições iguais (equals), o que permite conferir que o
 * reprocessamento de um evento não muda o resultado.
 */
public final class Posicao {

    private final int colocacao;
    private final Equipe equipe;
    private final int pontos;
    private final String criterioDecisivo;
    private final boolean pendenteDeSorteio;

    public Posicao(int colocacao, Equipe equipe, int pontos, String criterioDecisivo, boolean pendenteDeSorteio) {
        if (colocacao < 1) {
            throw new IllegalArgumentException("A colocação começa em 1.");
        }
        this.colocacao = colocacao;
        this.equipe = Objects.requireNonNull(equipe, "equipe");
        this.pontos = pontos;
        this.criterioDecisivo = Objects.requireNonNull(criterioDecisivo, "criterioDecisivo");
        this.pendenteDeSorteio = pendenteDeSorteio;
    }

    public int getColocacao() {
        return colocacao;
    }

    public Equipe getEquipe() {
        return equipe;
    }

    public int getPontos() {
        return pontos;
    }

    /** Critério que separou esta equipe das que estavam empatadas com ela. */
    public String getCriterioDecisivo() {
        return criterioDecisivo;
    }

    /** True quando nenhum critério numérico separou as equipes e falta o sorteio. */
    public boolean isPendenteDeSorteio() {
        return pendenteDeSorteio;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Posicao)) {
            return false;
        }
        Posicao p = (Posicao) outro;
        return colocacao == p.colocacao && pontos == p.pontos && pendenteDeSorteio == p.pendenteDeSorteio
                && equipe.equals(p.equipe) && criterioDecisivo.equals(p.criterioDecisivo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(colocacao, equipe, pontos, criterioDecisivo, pendenteDeSorteio);
    }

    @Override
    public String toString() {
        return colocacao + "º " + equipe + " (" + pontos + " pts, " + criterioDecisivo
                + (pendenteDeSorteio ? ", pendente de sorteio" : "") + ")";
    }
}
