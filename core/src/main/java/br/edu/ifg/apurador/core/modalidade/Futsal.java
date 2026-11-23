package br.edu.ifg.apurador.core.modalidade;

import br.edu.ifg.apurador.core.desempate.ConfrontoDireto;
import br.edu.ifg.apurador.core.desempate.CriterioDesempate;
import br.edu.ifg.apurador.core.desempate.MaiorSaldo;
import br.edu.ifg.apurador.core.desempate.MaisMarcados;
import br.edu.ifg.apurador.core.desempate.MaisVitorias;
import br.edu.ifg.apurador.core.desempate.MenosCartoesAmarelos;
import br.edu.ifg.apurador.core.desempate.MenosCartoesVermelhos;
import br.edu.ifg.apurador.core.desempate.MenosSofridos;
import br.edu.ifg.apurador.core.desempate.Sorteio;
import java.util.ArrayList;
import java.util.List;

/** Futsal. Pontuação e WO no Art. 61, desempate no Art. 62. */
public class Futsal extends ModalidadeDeGols {

    public Futsal() {
        super("Futsal");
    }

    /**
     * Art. 62, § 1º: lista para duas equipes.
     * Art. 62, § 2º: entre três ou mais, a mesma lista sem o confronto direto.
     * Por isso só o ConfrontoDireto tem a faixa limitada a duas equipes.
     */
    @Override
    public List<CriterioDesempate> criterios() {
        List<CriterioDesempate> lista = new ArrayList<>();
        lista.add(new ConfrontoDireto(this));
        lista.add(new MaisVitorias());
        lista.add(new MaiorSaldo());
        lista.add(new MaisMarcados());
        lista.add(new MenosSofridos());
        lista.add(new MenosCartoesVermelhos());
        lista.add(new MenosCartoesAmarelos());
        lista.add(new Sorteio());
        return lista;
    }
}
