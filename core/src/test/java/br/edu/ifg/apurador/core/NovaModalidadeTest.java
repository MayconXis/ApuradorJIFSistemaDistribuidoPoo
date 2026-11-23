package br.edu.ifg.apurador.core;

import static br.edu.ifg.apurador.core.Apoio.criterios;
import static br.edu.ifg.apurador.core.Apoio.jogo;
import static br.edu.ifg.apurador.core.Apoio.ordem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.ifg.apurador.core.desempate.ConfrontoDireto;
import br.edu.ifg.apurador.core.desempate.CriterioDesempate;
import br.edu.ifg.apurador.core.desempate.MaiorSaldo;
import br.edu.ifg.apurador.core.desempate.Sorteio;
import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import br.edu.ifg.apurador.core.modalidade.Modalidade;
import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.core.sumula.Partida;
import br.edu.ifg.apurador.core.sumula.Placar;
import br.edu.ifg.apurador.core.tabela.Chave;
import br.edu.ifg.apurador.core.tabela.Classificador;
import br.edu.ifg.apurador.core.tabela.Posicao;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TU-07. Extensibilidade (RNF-10) e testabilidade (RNF-11).
 * A modalidade abaixo é fictícia, criada só para o teste. Ela não representa
 * nenhuma regra do regulamento.
 */
@DisplayName("TU-07 Nova modalidade sem alterar o Classificador, e core sem Spring")
class NovaModalidadeTest {

    /** Modalidade inventada para o teste: vitória vale 2, empate 1, derrota 0. */
    static class ModalidadeFicticia extends Modalidade {

        ModalidadeFicticia() {
            super("Ficticia");
        }

        @Override
        public int pontosPara(Placar placar, Lado lado) {
            if (placar.empate()) {
                return 1;
            }
            return placar.vencedor() == lado ? 2 : 0;
        }

        @Override
        public Placar placarDeWO(Lado ausente, Collection<Partida> jogosDaChave) {
            return ausente == Lado.VISITANTE ? new Placar(1, 0) : new Placar(0, 1);
        }

        @Override
        public List<CriterioDesempate> criterios() {
            List<CriterioDesempate> lista = new ArrayList<>();
            lista.add(new ConfrontoDireto(this));
            lista.add(new MaiorSaldo());
            lista.add(new Sorteio());
            return lista;
        }
    }

    @Test
    @DisplayName("RNF-10: o Classificador classifica uma modalidade que ele nunca viu")
    void classificaModalidadeNova() {
        Modalidade ficticia = new ModalidadeFicticia();
        Equipe a = new Equipe("A");
        Equipe b = new Equipe("B");
        Equipe c = new Equipe("C");
        Chave chave = new Chave("Chave X", ficticia, Arrays.asList(a, b, c));
        chave.registrar(jogo(a, 3, 0, b));
        chave.registrar(jogo(b, 2, 0, c));
        chave.registrar(jogo(c, 1, 0, a));

        List<Posicao> classificacao = new Classificador().classificar(chave, ficticia);

        // Todas com 2 pontos. Confronto direto não vale para três. Saldo: A +2, B -1, C -1.
        // B e C voltam ao confronto direto: B venceu C.
        assertEquals(Arrays.asList("A", "B", "C"), ordem(classificacao));
        assertEquals(Arrays.asList("Saldo de gols", "Confronto direto", "Confronto direto"), criterios(classificacao));
        assertEquals(2, classificacao.get(0).getPontos());
    }

    @Test
    @DisplayName("RNF-10: a modalidade nova entra no catálogo com uma linha")
    void entraNoCatalogo() {
        CatalogoDeModalidades catalogo = CatalogoDeModalidades.padrao();
        Modalidade ficticia = new ModalidadeFicticia();
        catalogo.registrar(ficticia);

        assertSame(ficticia, catalogo.buscar("ficticia"));
        assertEquals("Futsal", catalogo.buscar("FUTSAL").getNome());
        assertEquals(4, catalogo.todas().size());
        assertThrows(IllegalArgumentException.class, () -> catalogo.buscar("xadrez"));
    }

    @Test
    @DisplayName("RNF-11: os testes do core rodam sem nenhuma classe do Spring no caminho")
    void coreSemSpring() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.springframework.context.ApplicationContext"));
    }
}
