package br.edu.ifg.apurador.core.tabela;

import br.edu.ifg.apurador.core.sumula.Equipe;

/**
 * A linha de uma equipe na tabela da chave.
 *
 * Encapsulamento: os atributos são private e não existem setters. A campanha só
 * muda pelo método registrar, que tem visibilidade de pacote (sem modificador):
 * só a Chave, que está no mesmo pacote, consegue chamá-lo. Assim, ponto só nasce
 * de partida registrada.
 */
public class Campanha {

    private final Equipe equipe;
    private int pontos;
    private int jogos;
    private int vitorias;
    private int empates;
    private int derrotas;

    /** Construtor com visibilidade de pacote: só a Chave cria campanhas. */
    Campanha(Equipe equipe) {
        this.equipe = equipe;
    }

    /**
     * Soma um jogo à campanha.
     *
     * @param pontosGanhos pontos que o jogo valeu para a equipe
     * @param resultado positivo para vitória, zero para empate, negativo para derrota
     */
    void registrar(int pontosGanhos, int resultado) {
        pontos += pontosGanhos;
        jogos++;
        if (resultado > 0) {
            vitorias++;
        } else if (resultado == 0) {
            empates++;
        } else {
            derrotas++;
        }
    }

    public Equipe getEquipe() {
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

    @Override
    public String toString() {
        return equipe + ": " + pontos + " pts em " + jogos + " jogos";
    }
}
