package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/**
 * Contas feitas sobre uma coleção de partidas. Classe utilitária: só métodos static,
 * construtor private. Recebe Collection porque só percorre os jogos.
 *
 * Partidas sem placar são ignoradas. Quem chama decide quais jogos entram:
 * todos os da chave ou só os jogos entre as equipes empatadas.
 */
public final class Estatisticas {

    private Estatisticas() {
    }

    public static int jogos(Equipe equipe, Collection<Partida> jogos) {
        int total = 0;
        for (Partida p : jogos) {
            if (conta(p, equipe)) {
                total++;
            }
        }
        return total;
    }

    public static int vitorias(Equipe equipe, Collection<Partida> jogos) {
        int total = 0;
        for (Partida p : jogos) {
            if (conta(p, equipe) && p.getPlacar().vencedor() == p.ladoDe(equipe)) {
                total++;
            }
        }
        return total;
    }

    public static int empates(Equipe equipe, Collection<Partida> jogos) {
        int total = 0;
        for (Partida p : jogos) {
            if (conta(p, equipe) && p.getPlacar().empate()) {
                total++;
            }
        }
        return total;
    }

    /** Gols (ou sets, no voleibol) a favor. */
    public static int marcados(Equipe equipe, Collection<Partida> jogos) {
        int total = 0;
        for (Partida p : jogos) {
            if (conta(p, equipe)) {
                total += p.getPlacar().de(p.ladoDe(equipe));
            }
        }
        return total;
    }

    /** Gols (ou sets, no voleibol) contra. */
    public static int sofridos(Equipe equipe, Collection<Partida> jogos) {
        int total = 0;
        for (Partida p : jogos) {
            if (conta(p, equipe)) {
                total += p.getPlacar().contra(p.ladoDe(equipe));
            }
        }
        return total;
    }

    public static int saldo(Equipe equipe, Collection<Partida> jogos) {
        return marcados(equipe, jogos) - sofridos(equipe, jogos);
    }

    /** Pontos de rali a favor, somando as parciais dos sets (voleibol). */
    public static int pontosNosSetsPro(Equipe equipe, Collection<Partida> jogos) {
        int total = 0;
        for (Partida p : jogos) {
            if (conta(p, equipe)) {
                total += p.getPlacar().pontosNosSets(p.ladoDe(equipe));
            }
        }
        return total;
    }

    /** Pontos de rali contra, somando as parciais dos sets (voleibol). */
    public static int pontosNosSetsContra(Equipe equipe, Collection<Partida> jogos) {
        int total = 0;
        for (Partida p : jogos) {
            if (conta(p, equipe)) {
                total += p.getPlacar().pontosNosSets(p.ladoDe(equipe).oposto());
            }
        }
        return total;
    }

    public static int amarelos(Equipe equipe, Collection<Partida> jogos) {
        int total = 0;
        for (Partida p : jogos) {
            if (p.envolve(equipe)) {
                total += p.getDisciplina().amarelos(p.ladoDe(equipe));
            }
        }
        return total;
    }

    public static int vermelhos(Equipe equipe, Collection<Partida> jogos) {
        int total = 0;
        for (Partida p : jogos) {
            if (p.envolve(equipe)) {
                total += p.getDisciplina().vermelhos(p.ladoDe(equipe));
            }
        }
        return total;
    }

    public static int sancoes(Equipe equipe, Collection<Partida> jogos) {
        int total = 0;
        for (Partida p : jogos) {
            if (p.envolve(equipe)) {
                total += p.getDisciplina().sancoes(p.ladoDe(equipe));
            }
        }
        return total;
    }

    /** Pontos na tabela, pela regra de pontuação informada. */
    public static int pontos(Equipe equipe, Collection<Partida> jogos, RegraDePontuacao regra) {
        int total = 0;
        for (Partida p : jogos) {
            if (conta(p, equipe)) {
                Lado lado = p.ladoDe(equipe);
                total += regra.pontosPara(p.getPlacar(), lado);
            }
        }
        return total;
    }

    private static boolean conta(Partida partida, Equipe equipe) {
        return partida.temPlacar() && partida.envolve(equipe);
    }
}
