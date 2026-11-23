package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/** Maior número de gols marcados. */
public final class MaisMarcados extends CriterioBase {

    /** Vale para qualquer empate e olha todos os jogos da chave. */
    public MaisMarcados() {
        this(2, SEM_LIMITE, false);
    }

    public MaisMarcados(int minimo, int maximo, boolean restrito) {
        super(rotulo("Gols marcados", restrito), minimo, maximo, restrito);
    }

    @Override
    public int comparar(Equipe a, Equipe b, Collection<Partida> jogos) {
        return maiorNaFrente(Estatisticas.marcados(a, jogos), Estatisticas.marcados(b, jogos));
    }
}
