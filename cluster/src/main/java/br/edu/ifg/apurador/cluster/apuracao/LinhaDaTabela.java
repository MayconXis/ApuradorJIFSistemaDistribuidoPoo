package br.edu.ifg.apurador.cluster.apuracao;

import java.util.Objects;

/**
 * Uma linha da classificação pronta para gravar e exibir: a Posicao do core
 * mais os números da campanha. Imutável, com equals: duas apurações dos mesmos
 * jogos produzem linhas iguais.
 */
public final class LinhaDaTabela {

    private final int colocacao;
    private final String equipe;
    private final int pontos;
    private final int jogos;
    private final int vitorias;
    private final int empates;
    private final int derrotas;
    private final int pro;
    private final int contra;
    private final String criterioDecisivo;
    private final boolean pendenteDeSorteio;

    public LinhaDaTabela(int colocacao, String equipe, int pontos, int jogos, int vitorias, int empates, int derrotas,
                         int pro, int contra, String criterioDecisivo, boolean pendenteDeSorteio) {
        this.colocacao = colocacao;
        this.equipe = equipe;
        this.pontos = pontos;
        this.jogos = jogos;
        this.vitorias = vitorias;
        this.empates = empates;
        this.derrotas = derrotas;
        this.pro = pro;
        this.contra = contra;
        this.criterioDecisivo = criterioDecisivo;
        this.pendenteDeSorteio = pendenteDeSorteio;
    }

    public int getColocacao() {
        return colocacao;
    }

    public String getEquipe() {
        return equipe;
    }

    public int getPontos() {
        return pontos;
    }

    public int getJogos() {
        return jogos;
    }

    public int getVitorias() {
        return vitorias;
    }

    public int getEmpates() {
        return empates;
    }

    public int getDerrotas() {
        return derrotas;
    }

    /** Gols a favor. No voleibol, sets a favor. */
    public int getPro() {
        return pro;
    }

    /** Gols contra. No voleibol, sets contra. */
    public int getContra() {
        return contra;
    }

    public int getSaldo() {
        return pro - contra;
    }

    public String getCriterioDecisivo() {
        return criterioDecisivo;
    }

    public boolean isPendenteDeSorteio() {
        return pendenteDeSorteio;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof LinhaDaTabela)) {
            return false;
        }
        LinhaDaTabela l = (LinhaDaTabela) outro;
        return colocacao == l.colocacao && pontos == l.pontos && jogos == l.jogos && vitorias == l.vitorias
                && empates == l.empates && derrotas == l.derrotas && pro == l.pro && contra == l.contra
                && pendenteDeSorteio == l.pendenteDeSorteio && equipe.equals(l.equipe)
                && criterioDecisivo.equals(l.criterioDecisivo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(colocacao, equipe, pontos, jogos, vitorias, empates, derrotas, pro, contra,
                criterioDecisivo, pendenteDeSorteio);
    }

    @Override
    public String toString() {
        return colocacao + "º " + equipe + " " + pontos + " pts (" + criterioDecisivo + ")";
    }
}
