package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.Collection;

/**
 * Confronto direto: pontos somados só nos jogos entre as equipes empatadas.
 * Entre duas equipes, é o resultado do jogo entre elas. Entre três ou mais
 * (handebol, Art. 67, § 2º, II), é a tabela só com os jogos entre as empatadas.
 *
 * Sempre restrito aos empatados: o Classificador entrega só esses jogos.
 * Precisa da regra de pontuação porque os pontos mudam de modalidade para modalidade.
 */
public final class ConfrontoDireto extends CriterioBase {

    private final RegraDePontuacao regra;

    /** Confronto direto para o empate entre exatamente duas equipes. */
    public ConfrontoDireto(RegraDePontuacao regra) {
        this(regra, 2, 2);
    }

    public ConfrontoDireto(RegraDePontuacao regra, int minimo, int maximo) {
        super(maximo == 2 ? "Confronto direto" : "Pontos no confronto direto", minimo, maximo, true);
        if (regra == null) {
            throw new IllegalArgumentException("O confronto direto precisa da regra de pontuação.");
        }
        this.regra = regra;
    }

    @Override
    public int comparar(Equipe a, Equipe b, Collection<Partida> jogos) {
        return maiorNaFrente(Estatisticas.pontos(a, jogos, regra), Estatisticas.pontos(b, jogos, regra));
    }
}
