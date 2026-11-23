package br.edu.ifg.apurador.core;

import static br.edu.ifg.apurador.core.Apoio.criterios;
import static br.edu.ifg.apurador.core.Apoio.jogo;
import static br.edu.ifg.apurador.core.Apoio.ordem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifg.apurador.core.modalidade.Futsal;
import br.edu.ifg.apurador.core.modalidade.Handebol;
import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.tabela.Chave;
import br.edu.ifg.apurador.core.tabela.Classificador;
import br.edu.ifg.apurador.core.tabela.Posicao;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TU-05. Comportamento geral do Classificador (RF-05, RF-07, RF-08, RNF-03, SUP-06). */
@DisplayName("TU-05 Classificador: critério decisivo, sorteio pendente e determinismo")
class ClassificadorTest {

    private final Futsal futsal = new Futsal();
    private final Classificador classificador = new Classificador();
    private final Equipe a = new Equipe("A");
    private final Equipe b = new Equipe("B");
    private final Equipe c = new Equipe("C");

    @Test
    @DisplayName("RF-07: sem empate, o critério decisivo de todas as posições é Pontos")
    void semEmpate() {
        Chave chave = new Chave("Chave A", futsal, Arrays.asList(c, b, a));
        chave.registrar(jogo(a, 2, 0, b));
        chave.registrar(jogo(a, 1, 0, c));
        chave.registrar(jogo(b, 1, 0, c));

        List<Posicao> classificacao = classificador.classificar(chave, futsal);

        assertEquals(Arrays.asList("A", "B", "C"), ordem(classificacao));
        assertEquals(Arrays.asList("Pontos", "Pontos", "Pontos"), criterios(classificacao));
        assertEquals(6, classificacao.get(0).getPontos());
        assertEquals(3, classificacao.get(1).getPontos());
        assertEquals(0, classificacao.get(2).getPontos());
    }

    @Test
    @DisplayName("RF-08, SUP-06: empate em todos os critérios fica pendente de sorteio")
    void sorteioPendente() {
        Chave chave = new Chave("Chave B", futsal, Arrays.asList(b, a));
        chave.registrar(jogo(b, 1, 1, a));

        List<Posicao> classificacao = classificador.classificar(chave, futsal);

        assertEquals(Arrays.asList("A", "B"), ordem(classificacao), "ordem provisória alfabética");
        assertEquals(Arrays.asList("Sorteio", "Sorteio"), criterios(classificacao));
        assertTrue(classificacao.get(0).isPendenteDeSorteio());
        assertTrue(classificacao.get(1).isPendenteDeSorteio());
    }

    @Test
    @DisplayName("RF-08: posição resolvida por critério numérico não fica pendente")
    void resolvidaNaoFicaPendente() {
        Chave chave = new Chave("Chave C", futsal, Arrays.asList(a, b));
        chave.registrar(jogo(a, 1, 0, b));
        for (Posicao posicao : classificador.classificar(chave, futsal)) {
            assertFalse(posicao.isPendenteDeSorteio());
        }
    }

    @Test
    @DisplayName("RNF-03: os mesmos jogos sempre produzem as mesmas posições")
    void mesmoEventoMesmaPosicao() {
        Chave chave = new Chave("Chave D", futsal, Arrays.asList(a, b, c));
        chave.registrar(jogo(a, 1, 1, b));
        chave.registrar(jogo(a, 3, 0, c));
        chave.registrar(jogo(b, 1, 0, c));

        List<Posicao> primeira = classificador.classificar(chave, futsal);
        List<Posicao> segunda = new Classificador().classificar(chave, new Futsal());

        assertEquals(primeira, segunda);
        assertEquals(primeira.hashCode(), segunda.hashCode());
    }

    @Test
    @DisplayName("A modalidade informada tem de ser a da chave")
    void modalidadeDiferenteDaChave() {
        Chave chave = new Chave("Chave E", futsal, Arrays.asList(a, b));
        assertThrows(IllegalArgumentException.class, () -> classificador.classificar(chave, new Handebol()));
        assertEquals(classificador.classificar(chave, futsal), classificador.classificar(chave));
    }

    @Test
    @DisplayName("A classificação devolvida não pode ser alterada")
    void listaSoDeLeitura() {
        Chave chave = new Chave("Chave F", futsal, Arrays.asList(a, b));
        chave.registrar(jogo(a, 1, 0, b));
        List<Posicao> classificacao = classificador.classificar(chave, futsal);
        assertThrows(UnsupportedOperationException.class, () -> classificacao.remove(0));
    }
}
