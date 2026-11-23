package br.edu.ifg.apurador.core.modalidade;

import br.edu.ifg.apurador.core.desempate.ConfrontoDireto;
import br.edu.ifg.apurador.core.desempate.CriterioDesempate;
import br.edu.ifg.apurador.core.desempate.MaisVitorias;
import br.edu.ifg.apurador.core.desempate.PontosAverage;
import br.edu.ifg.apurador.core.desempate.SetsAverage;
import br.edu.ifg.apurador.core.desempate.Sorteio;
import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.core.sumula.Partida;
import br.edu.ifg.apurador.core.sumula.Placar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Voleibol na fase classificatória: melhor de 3 sets (Art. 111).
 * Não existe empate, e os pontos dependem do placar em sets (Art. 112).
 */
public class Voleibol extends Modalidade {

    private static final int SETS_PARA_VENCER = 2;

    public Voleibol() {
        super("Voleibol");
    }

    /**
     * Art. 112: vitória por 2x0 vale 3 pontos, vitória por 2x1 vale 2,
     * derrota por 1x2 vale 1 e derrota por 0x2 vale 0.
     */
    @Override
    public int pontosPara(Placar placar, Lado lado) {
        int setsPro = placar.de(lado);
        int setsContra = placar.contra(lado);
        if (setsPro > setsContra) {
            return setsContra == 0 ? 3 : 2;
        }
        return setsPro == 0 ? 0 : 1;
    }

    /** Art. 112, parágrafo único: 2 sets a 0, com parciais de 25x0. Não depende dos outros jogos. */
    @Override
    public Placar placarDeWO(Lado ausente, Collection<Partida> jogosDaChave) {
        int[][] parciais = ausente == Lado.VISITANTE
                ? new int[][] {{25, 0}, {25, 0}}
                : new int[][] {{0, 25}, {0, 25}};
        return Placar.emSets(parciais);
    }

    /** Sobrescreve o gancho de Modalidade: melhor de 3, sem empate, com as parciais. */
    @Override
    public void validar(Placar placar) {
        super.validar(placar);
        int maior = Math.max(placar.de(Lado.MANDANTE), placar.de(Lado.VISITANTE));
        int menor = Math.min(placar.de(Lado.MANDANTE), placar.de(Lado.VISITANTE));
        if (maior != SETS_PARA_VENCER || menor >= SETS_PARA_VENCER) {
            throw new IllegalArgumentException(
                    "No voleibol, a fase classificatória é em melhor de 3 sets: o placar deve ser 2x0 ou 2x1.");
        }
        if (!placar.temParciais() || placar.getParciais().length != maior + menor) {
            throw new IllegalArgumentException("Informe as parciais de cada set do voleibol.");
        }
    }

    /** Art. 113: § 1º para duas equipes; § 2º, sem o confronto direto, para três ou mais. */
    @Override
    public List<CriterioDesempate> criterios() {
        List<CriterioDesempate> lista = new ArrayList<>();
        lista.add(new ConfrontoDireto(this));
        lista.add(new MaisVitorias());
        lista.add(new SetsAverage());
        lista.add(new PontosAverage());
        lista.add(new Sorteio());
        return lista;
    }
}
