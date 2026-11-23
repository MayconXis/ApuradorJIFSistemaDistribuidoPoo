package br.edu.ifg.apurador.core.desempate;

/**
 * Parte comum dos critérios numéricos: nome, faixa de equipes empatadas em que o
 * critério vale e se ele olha só os jogos entre as empatadas.
 *
 * Classe abstrata que implementa a interface pela metade: resolve nome(),
 * aplicavelA() e restritoAosEmpatados(), e deixa comparar() para as subclasses.
 * O construtor é protected porque só as subclasses o usam.
 */
public abstract class CriterioBase implements CriterioDesempate {

    public static final int SEM_LIMITE = Integer.MAX_VALUE;

    private final String nome;
    private final int minimo;
    private final int maximo;
    private final boolean restrito;

    protected CriterioBase(String nome, int minimo, int maximo, boolean restrito) {
        if (minimo < 2 || maximo < minimo) {
            throw new IllegalArgumentException("Faixa de equipes empatadas inválida.");
        }
        this.nome = nome;
        this.minimo = minimo;
        this.maximo = maximo;
        this.restrito = restrito;
    }

    @Override
    public final String nome() {
        return nome;
    }

    @Override
    public final boolean aplicavelA(int qtdEquipesEmpatadas) {
        return qtdEquipesEmpatadas >= minimo && qtdEquipesEmpatadas <= maximo;
    }

    @Override
    public final boolean restritoAosEmpatados() {
        return restrito;
    }

    /** Monta o nome do critério conforme o escopo dos jogos. */
    protected static String rotulo(String base, boolean restrito) {
        return restrito ? base + " no confronto direto" : base;
    }

    /** Quem tem o maior valor fica na frente. */
    protected static int maiorNaFrente(int valorA, int valorB) {
        return Integer.compare(valorB, valorA);
    }

    /** Quem tem o menor valor fica na frente. */
    protected static int menorNaFrente(int valorA, int valorB) {
        return Integer.compare(valorA, valorB);
    }

    @Override
    public String toString() {
        return nome;
    }
}
