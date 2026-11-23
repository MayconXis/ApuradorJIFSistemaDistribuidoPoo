package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/** Sets average: sets vencidos divididos por sets perdidos (voleibol, Art. 113). */
public final class SetsAverage extends CriterioDeAverage {

    public SetsAverage() {
        this(2, SEM_LIMITE, false);
    }

    public SetsAverage(int minimo, int maximo, boolean restrito) {
        super(rotulo("Sets average", restrito), minimo, maximo, restrito);
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
