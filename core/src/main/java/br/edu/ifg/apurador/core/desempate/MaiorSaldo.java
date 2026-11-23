package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/** Maior saldo de gols (gols marcados menos gols sofridos). */
public final class MaiorSaldo extends CriterioBase {

    /** Vale para qualquer empate e olha todos os jogos da chave. */
    public MaiorSaldo() {
        this(2, SEM_LIMITE, false);
    }

    public MaiorSaldo(int minimo, int maximo, boolean restrito) {
        super(rotulo("Saldo de gols", restrito), minimo, maximo, restrito);
    }

    @Override
    public int comparar(Equipe a, Equipe b, Collection<Partida> jogos) {
        return maiorNaFrente(Estatisticas.saldo(a, jogos), Estatisticas.saldo(b, jogos));
    }
}
