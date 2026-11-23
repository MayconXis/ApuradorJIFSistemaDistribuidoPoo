package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/** Pontos average: pontos feitos nos sets divididos pelos pontos sofridos (voleibol, Art. 113). */
public final class PontosAverage extends CriterioDeAverage {

    public PontosAverage() {
        this(2, SEM_LIMITE, false);
    }

    public PontosAverage(int minimo, int maximo, boolean restrito) {
        super(rotulo("Pontos average", restrito), minimo, maximo, restrito);
    }

    @Override
    protected int pro(Equipe equipe, Collection<Partida> jogos) {
        return Estatisticas.pontosNosSetsPro(equipe, jogos);
    }

    @Override
    protected int contra(Equipe equipe, Collection<Partida> jogos) {
        return Estatisticas.pontosNosSetsContra(equipe, jogos);
    }
}
