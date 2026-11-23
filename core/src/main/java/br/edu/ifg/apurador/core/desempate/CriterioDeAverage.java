package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/**
 * Parte comum dos averages (gol average, sets average e pontos average).
 *
 * SUPOSIÇÃO SUP-03: o regulamento não traz a fórmula. Adotamos pró dividido por contra.
 * A comparação é feita sem dividir, para não cair em divisão por zero:
 * a/b maior que c/d é o mesmo que a*d maior que c*b.
 * Quem tem zero contra fica à frente de qualquer average finito. Entre duas equipes
 * com zero contra, fica à frente quem tem mais a favor.
 *
 * As subclasses só dizem o que é "pró" e o que é "contra" (método modelo).
 */
public abstract class CriterioDeAverage extends CriterioBase {

    protected CriterioDeAverage(String nome, int minimo, int maximo, boolean restrito) {
        super(nome, minimo, maximo, restrito);
    }

    protected abstract int pro(Equipe equipe, Collection<Partida> jogos);

    protected abstract int contra(Equipe equipe, Collection<Partida> jogos);

    @Override
    public final int comparar(Equipe a, Equipe b, Collection<Partida> jogos) {
        long proA = pro(a, jogos);
        long contraA = contra(a, jogos);
        long proB = pro(b, jogos);
        long contraB = contra(b, jogos);
        if (contraA == 0 && contraB == 0) {
            return Long.compare(proB, proA);
        }
        if (contraA == 0) {
            return -1;
        }
        if (contraB == 0) {
            return 1;
        }
        return Long.compare(proB * contraA, proA * contraB);
    }
}
