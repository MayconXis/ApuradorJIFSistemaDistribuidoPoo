package br.edu.ifg.apurador.core.sumula;

/**
 * Ocorrências disciplinares de uma partida, por lado. Imutável.
 * Futsal usa cartões amarelos e vermelhos (Art. 62).
 * Handebol usa a pontuação de sanções (Art. 67): amarelo 1, exclusão de 2 minutos 2,
 * desqualificação pelo terceiro 2 minutos 6, desqualificação direta 10,
 * desqualificação com cartão azul 15.
 * Cada array tem duas posições: índice 0 para o mandante e 1 para o visitante.
 */
public final class Disciplina {

    private final int[] amarelos;
    private final int[] vermelhos;
    private final int[] sancoes;

    public Disciplina(int amarelosMandante, int amarelosVisitante,
                      int vermelhosMandante, int vermelhosVisitante,
                      int sancoesMandante, int sancoesVisitante) {
        this.amarelos = new int[] {naoNegativo(amarelosMandante), naoNegativo(amarelosVisitante)};
        this.vermelhos = new int[] {naoNegativo(vermelhosMandante), naoNegativo(vermelhosVisitante)};
        this.sancoes = new int[] {naoNegativo(sancoesMandante), naoNegativo(sancoesVisitante)};
    }

    /** Partida sem cartões nem sanções. */
    public static Disciplina semOcorrencias() {
        return new Disciplina(0, 0, 0, 0, 0, 0);
    }

    /** Atalho para o futsal: só cartões. */
    public static Disciplina cartoes(int amarelosMandante, int amarelosVisitante,
                                     int vermelhosMandante, int vermelhosVisitante) {
        return new Disciplina(amarelosMandante, amarelosVisitante, vermelhosMandante, vermelhosVisitante, 0, 0);
    }

    /** Atalho para o handebol: só a pontuação de sanções. */
    public static Disciplina sancoes(int sancoesMandante, int sancoesVisitante) {
        return new Disciplina(0, 0, 0, 0, sancoesMandante, sancoesVisitante);
    }

    public int amarelos(Lado lado) {
        return amarelos[lado.ordinal()];
    }

    public int vermelhos(Lado lado) {
        return vermelhos[lado.ordinal()];
    }

    public int sancoes(Lado lado) {
        return sancoes[lado.ordinal()];
    }

    private static int naoNegativo(int valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa.");
        }
        return valor;
    }
}
