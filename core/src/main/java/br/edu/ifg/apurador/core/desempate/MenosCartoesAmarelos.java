package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/** Menor número de cartões amarelos (futsal, Art. 62). */
public final class MenosCartoesAmarelos extends CriterioBase {

    /** Vale para qualquer empate e olha todos os jogos da chave. */
    public MenosCartoesAmarelos() {
        this(2, SEM_LIMITE, false);
    }

    public MenosCartoesAmarelos(int minimo, int maximo, boolean restrito) {
        super(rotulo("Cartões amarelos", restrito), minimo, maximo, restrito);
    }

    @Override
    public int comparar(Equipe a, Equipe b, Collection<Partida> jogos) {
        return menorNaFrente(Estatisticas.amarelos(a, jogos), Estatisticas.amarelos(b, jogos));
    }
}
