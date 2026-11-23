package br.edu.ifg.apurador.core;

import static br.edu.ifg.apurador.core.Apoio.criterios;
import static br.edu.ifg.apurador.core.Apoio.jogo;
import static br.edu.ifg.apurador.core.Apoio.ordem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.ifg.apurador.core.modalidade.Futsal;
import br.edu.ifg.apurador.core.sumula.Disciplina;
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

/** TU-02. Futsal: pontuação, WO e desempate entre duas equipes (RN-03, RN-04, RN-05). */
@DisplayName("TU-02 Futsal: pontuação, WO e desempate entre duas equipes")
class FutsalTest {

    private final Futsal futsal = new Futsal();
    private final Classificador classificador = new Classificador();
    private final Equipe a = new Equipe("A");
    private final Equipe b = new Equipe("B");
    private final Equipe c = new Equipe("C");
    private final Equipe d = new Equipe("D");

    @Test
    @DisplayName("RN-03: vitória 3, empate 1, derrota 0 (Art. 61)")
    void pontuacao() {
        assertEquals(3, futsal.pontosPara(new Placar(2, 1), Lado.MANDANTE));
        assertEquals(0, futsal.pontosPara(new Placar(2, 1), Lado.VISITANTE));
        assertEquals(1, futsal.pontosPara(new Placar(2, 2), Lado.MANDANTE));
        assertEquals(1, futsal.pontosPara(new Placar(2, 2), Lado.VISITANTE));
    }

    @Test
    @DisplayName("RN-04, SUP-02: o WO usa o jogo de maior diferença de gols da chave")
    void woUsaOMaiorPlacarDaChave() {
        Chave chave = new Chave("Chave B", futsal, Arrays.asList(a, b, c, d));
        chave.registrar(jogo(a, 5, 1, b));
        chave.registrar(jogo(c, 2, 0, d));
        chave.registrar("WO-1", a, d);

        Partida wo = chave.partidasEfetivas().get(2);
        assertTrue(wo.isWo());
        assertEquals(new Placar(5, 1), wo.getPlacar());
        assertEquals(6, chave.campanhaDe(a).getPontos());
        assertEquals(0, chave.campanhaDe(d).getPontos());
    }

    @Test
    @DisplayName("RN-04: o placar do WO é recalculado quando chega um jogo maior")
    void placarDoWoAcompanhaOsOutrosJogos() {
        Chave chave = new Chave("Chave B", futsal, Arrays.asList(a, b, c, d));
        chave.registrar("WO-1", a, d);
        assertEquals(new Placar(1, 0), chave.partidasEfetivas().get(0).getPlacar(), "sem outros jogos: 1 a 0");

        chave.registrar(jogo(b, 2, 2, c));
        assertEquals(new Placar(1, 0), chave.partidasEfetivas().get(0).getPlacar(), "empate não serve de referência");

        chave.registrar(jogo(a, 0, 4, b));
        assertEquals(new Placar(4, 0), chave.partidasEfetivas().get(0).getPlacar(), "vale o 4 a 0");
    }

    @Test
    @DisplayName("RN-04: quando o ausente é o mandante, o placar fica do lado do visitante")
    void woComMandanteAusente() {
        Chave chave = new Chave("Chave B", futsal, Arrays.asList(a, b, c));
        chave.registrar(jogo(b, 3, 0, c));
        chave.registrar(Partida.porWO("WO-2", a, b, Lado.MANDANTE));

        assertEquals(new Placar(0, 3), chave.partidasEfetivas().get(1).getPlacar());
        assertEquals(0, chave.campanhaDe(a).getPontos());
        assertEquals(6, chave.campanhaDe(b).getPontos());
    }

    @Test
    @DisplayName("RN-05: entre duas equipes, o confronto direto vem primeiro (Art. 62, § 1º)")
    void duasEquipesDecididasNoConfrontoDireto() {
        Chave chave = new Chave("Chave C", futsal, Arrays.asList(a, b, c, d));
        chave.registrar(jogo(a, 1, 0, b));
        chave.registrar(jogo(c, 1, 0, a));
        chave.registrar(jogo(d, 1, 0, a));
        chave.registrar(jogo(b, 1, 0, c));
        chave.registrar(jogo(d, 1, 0, b));
        chave.registrar(jogo(c, 0, 0, d));

        List<Posicao> classificacao = classificador.classificar(chave, futsal);

        assertEquals(Arrays.asList("D", "C", "A", "B"), ordem(classificacao));
        assertEquals(Arrays.asList("Pontos", "Pontos", "Confronto direto", "Confronto direto"), criterios(classificacao));
    }

    @Test
    @DisplayName("RN-05: confronto direto empatado, decide o saldo de gols")
    void confrontoDiretoEmpatadoDecideNoSaldo() {
        Chave chave = new Chave("Chave D", futsal, Arrays.asList(a, b, c));
        chave.registrar(jogo(a, 1, 1, b));
        chave.registrar(jogo(a, 3, 0, c));
        chave.registrar(jogo(b, 1, 0, c));

        List<Posicao> classificacao = classificador.classificar(chave, futsal);

        assertEquals(Arrays.asList("A", "B", "C"), ordem(classificacao));
        assertEquals(Arrays.asList("Saldo de gols", "Saldo de gols", "Pontos"), criterios(classificacao));
    }

    @Test
    @DisplayName("RN-05: no futsal, gols marcados vêm antes de gols sofridos")
    void golsMarcadosAntesDeSofridos() {
        Chave chave = new Chave("Chave E", futsal, Arrays.asList(a, b, c));
        chave.registrar(jogo(a, 2, 2, b));
        chave.registrar(jogo(a, 5, 3, c));
        chave.registrar(jogo(b, 3, 1, c));

        List<Posicao> classificacao = classificador.classificar(chave, futsal);

        assertEquals(Arrays.asList("A", "B", "C"), ordem(classificacao));
        assertEquals("Gols marcados", classificacao.get(0).getCriterioDecisivo());
    }

    @Test
    @DisplayName("RN-05: tudo igual nos gols, decide quem tem menos cartões vermelhos")
    void decideNosCartoesVermelhos() {
        Chave chave = new Chave("Chave F", futsal, Arrays.asList(a, b, c));
        chave.registrar(jogo(a, 1, 1, b, Disciplina.cartoes(0, 0, 1, 0)));
        chave.registrar(jogo(a, 2, 0, c));
        chave.registrar(jogo(b, 2, 0, c));

        List<Posicao> classificacao = classificador.classificar(chave, futsal);

        assertEquals(Arrays.asList("B", "A", "C"), ordem(classificacao));
        assertEquals("Cartões vermelhos", classificacao.get(0).getCriterioDecisivo());
    }

    @Test
    @DisplayName("RN-05: vermelhos iguais, decide quem tem menos cartões amarelos")
    void decideNosCartoesAmarelos() {
        Chave chave = new Chave("Chave G", futsal, Arrays.asList(a, b, c));
        chave.registrar(jogo(a, 1, 1, b, Disciplina.cartoes(1, 3, 0, 0)));
        chave.registrar(jogo(a, 2, 0, c));
        chave.registrar(jogo(b, 2, 0, c));

        List<Posicao> classificacao = classificador.classificar(chave, futsal);

        assertEquals(Arrays.asList("A", "B", "C"), ordem(classificacao));
        assertEquals("Cartões amarelos", classificacao.get(0).getCriterioDecisivo());
    }

    @Test
    @DisplayName("RN-02: em rodízio simples, o mesmo par não joga duas vezes")
    void rodizioSimples() {
        Chave chave = new Chave("Chave H", futsal, Arrays.asList(a, b, c));
        chave.registrar(jogo(a, 1, 0, b));
        assertThrows(IllegalArgumentException.class, () -> chave.registrar(jogo(b, 2, 0, a)));
        assertThrows(IllegalArgumentException.class, () -> chave.registrar(jogo(a, 2, 0, d)));
    }
}
