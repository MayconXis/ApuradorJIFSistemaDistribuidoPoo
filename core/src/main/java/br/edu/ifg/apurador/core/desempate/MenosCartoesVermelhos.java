package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/** Menor número de cartões vermelhos (futsal, Art. 62). */
public final class MenosCartoesVermelhos extends CriterioBase {

    /** Vale para qualquer empate e olha todos os jogos da chave. */
    public MenosCartoesVermelhos() {
        this(2, SEM_LIMITE, false);
    }

    public MenosCartoesVermelhos(int minimo, int maximo, boolean restrito) {
        super(rotulo("Cartões vermelhos", restrito), minimo, maximo, restrito);
    }

    @Override
    public int comparar(Equipe a, Equipe b, Collection<Partida> jogos) {
        return menorNaFrente(Estatisticas.vermelhos(a, jogos), Estatisticas.vermelhos(b, jogos));
    }
}
