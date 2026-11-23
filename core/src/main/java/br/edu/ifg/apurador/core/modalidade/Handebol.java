package br.edu.ifg.apurador.core.modalidade;

import br.edu.ifg.apurador.core.desempate.ConfrontoDireto;
import br.edu.ifg.apurador.core.desempate.CriterioBase;
import br.edu.ifg.apurador.core.desempate.CriterioDesempate;
import br.edu.ifg.apurador.core.desempate.GolAverage;
import br.edu.ifg.apurador.core.desempate.MaiorSaldo;
import br.edu.ifg.apurador.core.desempate.MaisMarcados;
import br.edu.ifg.apurador.core.desempate.MaisVitorias;
import br.edu.ifg.apurador.core.desempate.MenosSancoes;
import br.edu.ifg.apurador.core.desempate.MenosSofridos;
import br.edu.ifg.apurador.core.desempate.Sorteio;
import java.util.ArrayList;
import java.util.List;

/**
 * Handebol. Pontuação e WO no Art. 66, desempate no Art. 67.
 *
 * Diferente do futsal, o handebol tem duas listas: uma para duas equipes (§ 1º)
 * e outra, com 13 critérios, para três ou mais (§ 2º). As duas ficam na mesma
 * lista ordenada, e a faixa de cada critério diz em qual caso ele vale.
 */
public class Handebol extends ModalidadeDeGols {

    private static final int SEM_LIMITE = CriterioBase.SEM_LIMITE;
    private static final boolean NA_FASE = false;
    private static final boolean ENTRE_EMPATADAS = true;

    public Handebol() {
        super("Handebol");
    }

    @Override
    public List<CriterioDesempate> criterios() {
        List<CriterioDesempate> lista = new ArrayList<>();

        // Art. 67, § 1º: empate entre duas equipes.
        lista.add(new ConfrontoDireto(this));                       // I
        lista.add(new MaisVitorias(2, 2, NA_FASE));                 // II
        lista.add(new MaiorSaldo(2, 2, NA_FASE));                   // III
        lista.add(new MenosSofridos(2, 2, NA_FASE));                // IV
        lista.add(new MaisMarcados(2, 2, NA_FASE));                 // V
        lista.add(new GolAverage(2, 2, NA_FASE));                   // VI
        lista.add(new MenosSancoes(2, 2, NA_FASE));                 // VII

        // Art. 67, § 2º: empate entre três ou mais equipes.
        lista.add(new MaisVitorias(3, SEM_LIMITE, NA_FASE));        // I
        lista.add(new ConfrontoDireto(this, 3, SEM_LIMITE));        // II
        lista.add(new MaiorSaldo(3, SEM_LIMITE, ENTRE_EMPATADAS));  // III
        lista.add(new MenosSofridos(3, SEM_LIMITE, ENTRE_EMPATADAS)); // IV
        lista.add(new MaisMarcados(3, SEM_LIMITE, ENTRE_EMPATADAS)); // V
        lista.add(new MaiorSaldo(3, SEM_LIMITE, NA_FASE));          // VI
        lista.add(new MenosSofridos(3, SEM_LIMITE, NA_FASE));       // VII
        lista.add(new MaisMarcados(3, SEM_LIMITE, NA_FASE));        // VIII
        lista.add(new GolAverage(3, SEM_LIMITE, ENTRE_EMPATADAS));  // IX
        lista.add(new GolAverage(3, SEM_LIMITE, NA_FASE));          // X
        lista.add(new MenosSancoes(3, SEM_LIMITE, ENTRE_EMPATADAS)); // XI
        // XII, "sanções entre as equipes empatadas". SUPOSIÇÃO SUP-05: sanções em todos os jogos da fase.
        lista.add(new MenosSancoes(3, SEM_LIMITE, NA_FASE));        // XII

        // Sorteio: VIII do § 1º e XIII do § 2º.
        lista.add(new Sorteio());
        return lista;
    }
}
