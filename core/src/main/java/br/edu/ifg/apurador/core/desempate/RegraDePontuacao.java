package br.edu.ifg.apurador.core.desempate;

import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.core.sumula.Placar;

/**
 * Contrato de quem sabe quantos pontos um placar vale para um lado.
 * A classe abstrata Modalidade implementa esta interface. Com isso, os critérios
 * de desempate que precisam de pontos (como o confronto direto) dependem só
 * deste contrato, e não do pacote de modalidades.
 */
public interface RegraDePontuacao {

    int pontosPara(Placar placar, Lado lado);
}
