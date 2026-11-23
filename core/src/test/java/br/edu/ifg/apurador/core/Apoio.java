package br.edu.ifg.apurador.core;

import br.edu.ifg.apurador.core.sumula.Disciplina;
import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import br.edu.ifg.apurador.core.sumula.Placar;
import br.edu.ifg.apurador.core.tabela.Posicao;
import java.util.ArrayList;
import java.util.List;

/** Atalhos usados pelos testes do core. */
final class Apoio {

    private static int sequencia = 0;

    private Apoio() {
    }

    static Partida jogo(Equipe mandante, int golsMandante, int golsVisitante, Equipe visitante) {
        return Partida.jogada(proximoId(), mandante, visitante, new Placar(golsMandante, golsVisitante));
    }

    static Partida jogo(Equipe mandante, int golsMandante, int golsVisitante, Equipe visitante, Disciplina disciplina) {
        return Partida.jogada(proximoId(), mandante, visitante, new Placar(golsMandante, golsVisitante), disciplina);
    }

    /** Jogo de voleibol a partir das parciais: sets(a, b, 25, 20, 25, 18) é 2x0 para a. */
    static Partida sets(Equipe mandante, Equipe visitante, int... pontos) {
        int[][] parciais = new int[pontos.length / 2][2];
        for (int i = 0; i < parciais.length; i++) {
            parciais[i][0] = pontos[2 * i];
            parciais[i][1] = pontos[2 * i + 1];
        }
        return Partida.jogada(proximoId(), mandante, visitante, Placar.emSets(parciais));
    }

    static String proximoId() {
        sequencia++;
        return "J" + sequencia;
    }

    /** Nomes das equipes na ordem da classificação. */
    static List<String> ordem(List<Posicao> classificacao) {
        List<String> nomes = new ArrayList<>();
        for (Posicao posicao : classificacao) {
            nomes.add(posicao.getEquipe().getNome());
        }
        return nomes;
    }

    /** Critério decisivo de cada posição, na ordem da classificação. */
    static List<String> criterios(List<Posicao> classificacao) {
        List<String> nomes = new ArrayList<>();
        for (Posicao posicao : classificacao) {
            nomes.add(posicao.getCriterioDecisivo());
        }
        return nomes;
    }
}
