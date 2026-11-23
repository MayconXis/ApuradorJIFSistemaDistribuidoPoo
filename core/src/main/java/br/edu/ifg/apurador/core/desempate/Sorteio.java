package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/**
 * Último critério de todas as listas do regulamento.
 *
 * SUPOSIÇÃO SUP-06: o sorteio é feito fora do sistema. Por isso comparar() sempre
 * devolve zero. Quando a lista chega até aqui sem separar as equipes, o Classificador
 * marca as posições como pendentes de sorteio.
 *
 * Implementa a interface direto, sem herdar de CriterioBase, porque não usa
 * nenhuma estatística.
 */
public final class Sorteio implements CriterioDesempate {

    @Override
    public String nome() {
        return "Sorteio";
    }

    @Override
    public boolean aplicavelA(int qtdEquipesEmpatadas) {
        return qtdEquipesEmpatadas >= 2;
    }

    @Override
    public boolean restritoAosEmpatados() {
        return false;
    }

    @Override
    public int comparar(Equipe a, Equipe b, Collection<Partida> jogos) {
        return 0;
    }

    @Override
    public String toString() {
        return nome();
    }
}
