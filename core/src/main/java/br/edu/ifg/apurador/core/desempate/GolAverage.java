package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/** Gol average: gols marcados divididos por gols sofridos (handebol, Art. 67). */
public final class GolAverage extends CriterioDeAverage {

    public GolAverage() {
        this(2, SEM_LIMITE, false);
    }

    public GolAverage(int minimo, int maximo, boolean restrito) {
        super(rotulo("Gol average", restrito), minimo, maximo, restrito);
    }

    @Override
    protected int pro(Equipe equipe, Collection<Partida> jogos) {
        return Estatisticas.marcados(equipe, jogos);
    }

    @Override
    protected int contra(Equipe equipe, Collection<Partida> jogos) {
        return Estatisticas.sofridos(equipe, jogos);
    }
}
