package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/**
 * Contrato de um critério de desempate. Cada critério do regulamento é uma classe
 * que implementa esta interface. O Classificador trabalha só com este tipo,
 * sem saber qual classe está por trás (polimorfismo).
 */
public interface CriterioDesempate {

    /** Nome que aparece na tabela como critério decisivo. */
    String nome();

    /** O critério vale para um empate entre esta quantidade de equipes? */
    boolean aplicavelA(int qtdEquipesEmpatadas);

    /**
     * Se true, o Classificador entrega ao critério só os jogos entre as equipes
     * empatadas (confronto direto). Se false, entrega todos os jogos da chave.
     */
    boolean restritoAosEmpatados();

    /**
     * Compara duas equipes empatadas.
     * Negativo: a fica na frente. Positivo: b fica na frente. Zero: o critério não separa.
     */
    int comparar(Equipe a, Equipe b, Collection<Partida> jogos);
}
