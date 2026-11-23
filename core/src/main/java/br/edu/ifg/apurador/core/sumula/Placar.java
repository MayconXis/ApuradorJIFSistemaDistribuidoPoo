package br.edu.ifg.apurador.core.sumula;

import java.util.Arrays;

/**
 * Placar de uma partida. Imutável: todos os atributos são final, não há setters
 * e o array de parciais é copiado na entrada e na saída.
 *
 * No futsal e no handebol, mandante e visitante guardam gols.
 * No voleibol, guardam sets vencidos, e parciais guarda os pontos de cada set:
 * parciais[set][0] são os pontos do mandante e parciais[set][1], os do visitante.
 */
public final class Placar {

    private final int mandante;
    private final int visitante;
    private final int[][] parciais;

    /** Placar em gols (futsal e handebol). */
    public Placar(int mandante, int visitante) {
        this(mandante, visitante, new int[0][2]);
    }

    private Placar(int mandante, int visitante, int[][] parciais) {
        if (mandante < 0 || visitante < 0) {
            throw new IllegalArgumentException("Placar não pode ser negativo.");
        }
        this.mandante = mandante;
        this.visitante = visitante;
        this.parciais = copiar(parciais);
    }

    /**
     * Placar em sets (voleibol), calculado a partir das parciais.
     * Exemplo: {{25, 20}, {18, 25}, {15, 10}} vira 2 sets a 1 para o mandante.
     */
    public static Placar emSets(int[][] parciais) {
        if (parciais == null || parciais.length == 0) {
            throw new IllegalArgumentException("Informe as parciais de pelo menos um set.");
        }
        int setsMandante = 0;
        int setsVisitante = 0;
        for (int[] set : parciais) {
            if (set == null || set.length != 2 || set[0] < 0 || set[1] < 0) {
                throw new IllegalArgumentException("Cada set precisa de dois valores não negativos.");
            }
            if (set[0] == set[1]) {
                throw new IllegalArgumentException("Um set não termina empatado.");
            }
            if (set[0] > set[1]) {
                setsMandante++;
            } else {
                setsVisitante++;
            }
        }
        return new Placar(setsMandante, setsVisitante, parciais);
    }

    /** Gols ou sets do lado informado. */
    public int de(Lado lado) {
        return lado == Lado.MANDANTE ? mandante : visitante;
    }

    /** Gols ou sets sofridos pelo lado informado. */
    public int contra(Lado lado) {
        return de(lado.oposto());
    }

    /** Lado vencedor, ou null em caso de empate. */
    public Lado vencedor() {
        if (mandante == visitante) {
            return null;
        }
        return mandante > visitante ? Lado.MANDANTE : Lado.VISITANTE;
    }

    public boolean empate() {
        return mandante == visitante;
    }

    public int diferenca() {
        return Math.abs(mandante - visitante);
    }

    public boolean temParciais() {
        return parciais.length > 0;
    }

    /** Cópia das parciais: quem recebe não consegue alterar o placar. */
    public int[][] getParciais() {
        return copiar(parciais);
    }

    /** Soma dos pontos que o lado fez em todos os sets (voleibol). */
    public int pontosNosSets(Lado lado) {
        int indice = lado == Lado.MANDANTE ? 0 : 1;
        int soma = 0;
        for (int[] set : parciais) {
            soma += set[indice];
        }
        return soma;
    }

    private static int[][] copiar(int[][] origem) {
        int[][] copia = new int[origem.length][];
        for (int i = 0; i < origem.length; i++) {
            copia[i] = Arrays.copyOf(origem[i], origem[i].length);
        }
        return copia;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Placar)) {
            return false;
        }
        Placar p = (Placar) outro;
        return mandante == p.mandante && visitante == p.visitante && Arrays.deepEquals(parciais, p.parciais);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * mandante + visitante) + Arrays.deepHashCode(parciais);
    }

    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder(mandante + "x" + visitante);
        if (temParciais()) {
            texto.append(" (");
            for (int i = 0; i < parciais.length; i++) {
                texto.append(i > 0 ? ", " : "").append(parciais[i][0]).append("-").append(parciais[i][1]);
            }
            texto.append(")");
        }
        return texto.toString();
    }
}
