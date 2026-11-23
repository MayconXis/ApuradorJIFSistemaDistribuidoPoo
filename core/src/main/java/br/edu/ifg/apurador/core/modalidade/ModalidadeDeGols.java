package br.edu.ifg.apurador.core.modalidade;

import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.core.sumula.Partida;
import br.edu.ifg.apurador.core.sumula.Placar;
import java.util.Collection;

/**
 * Modalidades decididas em gols, com vitória, empate e derrota: futsal e handebol.
 * Segundo nível de herança: guarda o que as duas têm em comum (Arts. 61 e 66),
 * e cada uma só escreve a sua lista de critérios.
 */
public abstract class ModalidadeDeGols extends Modalidade {

    protected static final int PONTOS_VITORIA = 3;
    protected static final int PONTOS_EMPATE = 1;
    protected static final int PONTOS_DERROTA = 0;

    protected ModalidadeDeGols(String nome) {
        super(nome);
    }

    @Override
    public int pontosPara(Placar placar, Lado lado) {
        Lado vencedor = placar.vencedor();
        if (vencedor == null) {
            return PONTOS_EMPATE;
        }
        return vencedor == lado ? PONTOS_VITORIA : PONTOS_DERROTA;
    }

    /**
     * "No caso de WO, o placar adotado para o vencedor será o maior da fase dentro
     * da chave" (Art. 61 e Art. 66, parágrafo único).
     *
     * SUPOSIÇÃO SUP-02: o regulamento não diz o que é "o maior". Adotamos o jogo
     * com a maior diferença de gols entre os que tiveram vencedor. Se dois jogos
     * têm a mesma diferença, vale o de mais gols do vencedor. Se nenhum jogo da
     * chave teve vencedor ainda, vale 1 a 0. Outros WO não entram na conta.
     */
    @Override
    public Placar placarDeWO(Lado ausente, Collection<Partida> jogosDaChave) {
        int golsDoVencedor = 1;
        int golsDoPerdedor = 0;
        int maiorDiferenca = 0;
        for (Partida partida : jogosDaChave) {
            if (partida.isWo() || !partida.temPlacar()) {
                continue;
            }
            Placar placar = partida.getPlacar();
            Lado vencedor = placar.vencedor();
            if (vencedor == null) {
                continue;
            }
            int diferenca = placar.diferenca();
            int gols = placar.de(vencedor);
            if (diferenca > maiorDiferenca || (diferenca == maiorDiferenca && gols > golsDoVencedor)) {
                maiorDiferenca = diferenca;
                golsDoVencedor = gols;
                golsDoPerdedor = placar.contra(vencedor);
            }
        }
        return ausente == Lado.VISITANTE
                ? new Placar(golsDoVencedor, golsDoPerdedor)
                : new Placar(golsDoPerdedor, golsDoVencedor);
    }
}
