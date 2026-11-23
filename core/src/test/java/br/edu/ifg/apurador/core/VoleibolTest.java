package br.edu.ifg.apurador.core;

import static br.edu.ifg.apurador.core.Apoio.criterios;
import static br.edu.ifg.apurador.core.Apoio.ordem;
import static br.edu.ifg.apurador.core.Apoio.sets;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifg.apurador.core.desempate.SetsAverage;
import br.edu.ifg.apurador.core.modalidade.Voleibol;
import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.core.sumula.Partida;
import br.edu.ifg.apurador.core.sumula.Placar;
import br.edu.ifg.apurador.core.tabela.Chave;
import br.edu.ifg.apurador.core.tabela.Classificador;
import br.edu.ifg.apurador.core.tabela.Posicao;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TU-03. Voleibol: pontos por placar em sets, WO, validação e averages (RN-11 a RN-14, SUP-03). */
@DisplayName("TU-03 Voleibol: pontos por sets, WO, validação e averages")
class VoleibolTest {

    private final Voleibol voleibol = new Voleibol();
    private final Classificador classificador = new Classificador();
    private final Equipe a = new Equipe("A");
    private final Equipe b = new Equipe("B");
    private final Equipe c = new Equipe("C");

    @Test
    @DisplayName("RN-12: 2x0 vale 3, 2x1 vale 2, 1x2 vale 1, 0x2 vale 0 (Art. 112)")
    void pontosPeloPlacarEmSets() {
        Placar doisAZero = Placar.emSets(new int[][] {{25, 20}, {25, 18}});
        Placar doisAUm = Placar.emSets(new int[][] {{25, 20}, {18, 25}, {15, 10}});

        assertEquals(3, voleibol.pontosPara(doisAZero, Lado.MANDANTE));
        assertEquals(0, voleibol.pontosPara(doisAZero, Lado.VISITANTE));
        assertEquals(2, voleibol.pontosPara(doisAUm, Lado.MANDANTE));
        assertEquals(1, voleibol.pontosPara(doisAUm, Lado.VISITANTE));
    }

    @Test
    @DisplayName("RN-13: WO vale 3 pontos, 2 sets a 0 e parciais de 25x0 (Art. 112, parágrafo único)")
    void wo() {
        Chave chave = new Chave("Chave A", voleibol, Arrays.asList(a, b, c));
        chave.registrar("WO-1", a, b);

        Placar placar = chave.partidasEfetivas().get(0).getPlacar();
        assertEquals(2, placar.de(Lado.MANDANTE));
        assertEquals(0, placar.de(Lado.VISITANTE));
        assertArrayEquals(new int[][] {{25, 0}, {25, 0}}, placar.getParciais());
        assertEquals(3, chave.campanhaDe(a).getPontos());
        assertEquals(0, chave.campanhaDe(b).getPontos());
    }

    @Test
    @DisplayName("RF-03, RN-11: placar impossível na melhor de 3 é recusado (Art. 111)")
    void validacao() {
        Chave chave = new Chave("Chave A", voleibol, Arrays.asList(a, b, c));

        assertThrows(IllegalArgumentException.class, () -> chave.registrar(sets(a, b, 25, 20, 20, 25)), "1 a 1");
        assertThrows(IllegalArgumentException.class, () -> chave.registrar(sets(a, b, 25, 1, 25, 2, 25, 3)), "3 a 0");
        assertThrows(IllegalArgumentException.class,
                () -> chave.registrar(Partida.jogada("X", a, b, new Placar(2, 0))), "sem parciais");
        assertThrows(IllegalArgumentException.class, () -> Placar.emSets(new int[][] {{25, 25}}), "set empatado");
        assertTrue(chave.getPartidas().isEmpty(), "nada foi registrado");
    }

    @Test
    @DisplayName("RN-14: entre duas equipes, decide o confronto direto (Art. 113, § 1º)")
    void duasEquipesNoConfrontoDireto() {
        Chave chave = new Chave("Chave A", voleibol, Arrays.asList(a, b, c));
        chave.registrar(sets(a, b, 25, 20, 20, 25, 15, 13));
        chave.registrar(sets(b, c, 25, 10, 25, 12));
        chave.registrar(sets(a, c, 25, 22, 23, 25, 15, 9));

        List<Posicao> classificacao = classificador.classificar(chave, voleibol);

        assertEquals(4, chave.campanhaDe(a).getPontos());
        assertEquals(4, chave.campanhaDe(b).getPontos());
        assertEquals(Arrays.asList("A", "B", "C"), ordem(classificacao));
        assertEquals(Arrays.asList("Confronto direto", "Confronto direto", "Pontos"), criterios(classificacao));
    }

    @Test
    @DisplayName("RN-14: empate triplo sem confronto direto, decide o pontos average (Art. 113, § 2º)")
    void empateTriploNoPontosAverage() {
        Chave chave = new Chave("Chave B", voleibol, Arrays.asList(a, b, c));
        chave.registrar(sets(a, b, 25, 20, 25, 20));
        chave.registrar(sets(b, c, 25, 10, 25, 10));
        chave.registrar(sets(c, a, 25, 23, 25, 23));

        List<Posicao> classificacao = classificador.classificar(chave, voleibol);

        // Vitórias: 1 cada. Sets: 2 a 2 cada. Pontos: A 96/90, B 90/70, C 70/96.
        assertEquals(Arrays.asList("B", "A", "C"), ordem(classificacao));
        assertEquals(Arrays.asList("Pontos average", "Pontos average", "Pontos average"), criterios(classificacao));
    }

    @Test
    @DisplayName("SUP-03: average é pró dividido por contra, e zero contra fica na frente")
    void averageSemDivisaoPorZero() {
        Equipe d = new Equipe("D");
        SetsAverage criterio = new SetsAverage();
        // A: 4 sets a 1. B: 4 sets a 2. C: 2 sets a 0. D: 4 sets a 0.
        List<Partida> jogos = Arrays.asList(
                sets(a, new Equipe("X1"), 25, 0, 25, 0),
                sets(a, new Equipe("X2"), 25, 0, 0, 25, 15, 0),
                sets(b, new Equipe("X3"), 25, 0, 0, 25, 15, 0),
                sets(b, new Equipe("X4"), 25, 0, 0, 25, 15, 0),
                sets(c, new Equipe("X5"), 25, 0, 25, 0),
                sets(d, new Equipe("X6"), 25, 0, 25, 0),
                sets(d, new Equipe("X7"), 25, 0, 25, 0));

        assertTrue(criterio.comparar(a, b, jogos) < 0, "4/1 fica na frente de 4/2");
        assertTrue(criterio.comparar(c, a, jogos) < 0, "zero contra fica na frente de qualquer average finito");
        assertTrue(criterio.comparar(d, c, jogos) < 0, "entre dois com zero contra, quem tem mais a favor");
        assertEquals(0, criterio.comparar(a, a, jogos));
    }
}
