package br.edu.ifg.apurador.core.modalidade;

import br.edu.ifg.apurador.core.desempate.CriterioDesempate;
import br.edu.ifg.apurador.core.desempate.RegraDePontuacao;
import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.core.sumula.Partida;
import br.edu.ifg.apurador.core.sumula.Placar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Uma modalidade coletiva do JIF. Classe abstrata: o que muda de uma modalidade
 * para outra é comportamento (pontuação, placar do WO, critérios de desempate),
 * e cada subclasse escreve a sua versão desses métodos.
 *
 * Para criar uma modalidade nova basta uma subclasse. O Classificador não muda.
 */
public abstract class Modalidade implements RegraDePontuacao {

    private final String nome;

    /** Construtor protected: só as subclasses criam modalidades. */
    protected Modalidade(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("A modalidade precisa de um nome.");
        }
        this.nome = nome.trim();
    }

    /** Pontos na tabela que o placar vale para o lado informado. */
    @Override
    public abstract int pontosPara(Placar placar, Lado lado);

    /**
     * Critérios de desempate na ordem do regulamento. A ordem da lista é a regra.
     * Cada critério diz para quantas equipes empatadas ele vale.
     */
    public abstract List<CriterioDesempate> criterios();

    /**
     * Placar a considerar em uma vitória por WO.
     *
     * @param ausente lado que não compareceu
     * @param jogosDaChave jogos da chave, usados quando o placar depende dos outros jogos
     */
    public abstract Placar placarDeWO(Lado ausente, Collection<Partida> jogosDaChave);

    /**
     * Confere se o placar é possível nesta modalidade. Lança IllegalArgumentException
     * com o motivo quando não é. As subclasses podem sobrescrever para regras próprias.
     */
    public void validar(Placar placar) {
        if (placar == null) {
            throw new IllegalArgumentException("Informe o placar.");
        }
    }

    public final String getNome() {
        return nome;
    }

    /** Código usado para guardar a modalidade no banco, por exemplo FUTSAL. */
    public final String getCodigo() {
        return nome.toUpperCase(Locale.ROOT);
    }

    /** Duas instâncias da mesma subclasse são a mesma modalidade. */
    @Override
    public boolean equals(Object outro) {
        return outro != null && getClass() == outro.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return nome;
    }
}
