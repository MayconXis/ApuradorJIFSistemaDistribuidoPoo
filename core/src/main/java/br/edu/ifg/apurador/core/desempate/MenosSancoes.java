package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/** Menor pontuação de sanções (handebol, Art. 67). */
public final class MenosSancoes extends CriterioBase {

    /** Vale para qualquer empate e olha todos os jogos da chave. */
    public MenosSancoes() {
        this(2, SEM_LIMITE, false);
    }

    public MenosSancoes(int minimo, int maximo, boolean restrito) {
        super(rotulo("Sanções", restrito), minimo, maximo, restrito);
    }

    @Override
    public int comparar(Equipe a, Equipe b, Collection<Partida> jogos) {
        return menorNaFrente(Estatisticas.sancoes(a, jogos), Estatisticas.sancoes(b, jogos));
    }
}
