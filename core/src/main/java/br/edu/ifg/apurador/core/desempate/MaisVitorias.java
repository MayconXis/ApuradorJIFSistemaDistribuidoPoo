package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/** Maior número de vitórias. */
public final class MaisVitorias extends CriterioBase {

    /** Vale para qualquer empate e olha todos os jogos da chave. */
    public MaisVitorias() {
        this(2, SEM_LIMITE, false);
    }

    public MaisVitorias(int minimo, int maximo, boolean restrito) {
        super(rotulo("Vitórias", restrito), minimo, maximo, restrito);
    }

    @Override
    public int comparar(Equipe a, Equipe b, Collection<Partida> jogos) {
        return maiorNaFrente(Estatisticas.vitorias(a, jogos), Estatisticas.vitorias(b, jogos));
    }
}
