package br.edu.ifg.apurador.core;

import static br.edu.ifg.apurador.core.Apoio.jogo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifg.apurador.core.modalidade.Futsal;
import br.edu.ifg.apurador.core.sumula.Disciplina;
import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.core.sumula.Partida;
import br.edu.ifg.apurador.core.sumula.Placar;
import br.edu.ifg.apurador.core.tabela.Campanha;
import br.edu.ifg.apurador.core.tabela.Chave;
import br.edu.ifg.apurador.core.tabela.Posicao;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TU-06. Imutabilidade de Placar e Posicao e encapsulamento da Campanha. */
@DisplayName("TU-06 Imutabilidade e encapsulamento")
class ImutabilidadeTest {

    @Test
    @DisplayName("Placar copia as parciais na entrada e na saída")
    void placarNaoVazaOArray() {
        int[][] parciais = {{25, 20}, {25, 18}};
        Placar placar = Placar.emSets(parciais);

        parciais[0][0] = 0;
        placar.getParciais()[1][1] = 99;

        assertEquals(2, placar.de(Lado.MANDANTE));
        assertEquals(50, placar.pontosNosSets(Lado.MANDANTE));
        assertEquals(38, placar.pontosNosSets(Lado.VISITANTE));
    }

    @Test
    @DisplayName("Placar, Posicao, Partida, Equipe e Disciplina: atributos private final e sem setters")
    void classesImutaveis() {
        for (Class<?> classe : Arrays.asList(Placar.class, Posicao.class, Partida.class, Equipe.class, Disciplina.class)) {
            assertTrue(Modifier.isFinal(classe.getModifiers()), classe.getSimpleName() + " deve ser final");
            for (Field atributo : classe.getDeclaredFields()) {
                if (Modifier.isStatic(atributo.getModifiers())) {
                    continue;
                }
                assertTrue(Modifier.isPrivate(atributo.getModifiers()), classe.getSimpleName() + "." + atributo.getName());
                assertTrue(Modifier.isFinal(atributo.getModifiers()), classe.getSimpleName() + "." + atributo.getName());
            }
            semSetters(classe);
        }
    }

    @Test
    @DisplayName("Campanha: atributos private, sem setters, e registrar não é público")
    void campanhaEncapsulada() throws Exception {
        for (Field atributo : Campanha.class.getDeclaredFields()) {
            assertTrue(Modifier.isPrivate(atributo.getModifiers()), atributo.getName());
        }
        semSetters(Campanha.class);
        Method registrar = Campanha.class.getDeclaredMethod("registrar", int.class, int.class);
        assertFalse(Modifier.isPublic(registrar.getModifiers()), "só a Chave, no mesmo pacote, registra");
        assertEquals(0, Campanha.class.getConstructors().length, "nenhum construtor público");
    }

    @Test
    @DisplayName("A lista de partidas da Chave é só de leitura")
    void partidasDaChaveSoDeLeitura() {
        Equipe a = new Equipe("A");
        Equipe b = new Equipe("B");
        Chave chave = new Chave("Chave A", new Futsal(), Arrays.asList(a, b));
        chave.registrar(jogo(a, 1, 0, b));

        assertThrows(UnsupportedOperationException.class, () -> chave.getPartidas().clear());
        assertThrows(UnsupportedOperationException.class, () -> chave.getEquipes().clear());
        assertEquals(3, chave.campanhaDe(a).getPontos());
    }

    private static void semSetters(Class<?> classe) {
        for (Method metodo : classe.getDeclaredMethods()) {
            assertFalse(metodo.getName().startsWith("set"), classe.getSimpleName() + "." + metodo.getName());
        }
    }
}
