package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/** Menor número de gols sofridos. */
public final class MenosSofridos extends CriterioBase {

    /** Vale para qualquer empate e olha todos os jogos da chave. */
    public MenosSofridos() {
        this(2, SEM_LIMITE, false);
    }

    public MenosSofridos(int minimo, int maximo, boolean restrito) {
        super(rotulo("Gols sofridos", restrito), minimo, maximo, restrito);
    }

    @Override
    public int comparar(Equipe a, Equipe b, Collection<Partida> jogos) {
        return menorNaFrente(Estatisticas.sofridos(a, jogos), Estatisticas.sofridos(b, jogos));
    }
}
