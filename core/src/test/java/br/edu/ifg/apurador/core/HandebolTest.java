package br.edu.ifg.apurador.core;

import static br.edu.ifg.apurador.core.Apoio.criterios;
import static br.edu.ifg.apurador.core.Apoio.jogo;
import static br.edu.ifg.apurador.core.Apoio.ordem;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.ifg.apurador.core.modalidade.Futsal;
import br.edu.ifg.apurador.core.modalidade.Handebol;
import br.edu.ifg.apurador.core.sumula.Disciplina;
import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.core.sumula.Placar;
import br.edu.ifg.apurador.core.tabela.Chave;
import br.edu.ifg.apurador.core.tabela.Classificador;
import br.edu.ifg.apurador.core.tabela.Posicao;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TU-04. Handebol: duas listas de critérios, confronto direto entre três e sanções (RN-07 a RN-10). */
@DisplayName("TU-04 Handebol: lista de 2, lista de 3 ou mais e sanções")
class HandebolTest {

    private final Handebol handebol = new Handebol();
    private final Classificador classificador = new Classificador();
    private final Equipe a = new Equipe("A");
    private final Equipe b = new Equipe("B");
    private final Equipe c = new Equipe("C");
    private final Equipe d = new Equipe("D");

    @Test
    @DisplayName("RN-07: vitória 3, empate 1, derrota 0 (Art. 66)")
    void pontuacao() {
        assertEquals(3, handebol.pontosPara(new Placar(28, 25), Lado.MANDANTE));
        assertEquals(0, handebol.pontosPara(new Placar(28, 25), Lado.VISITANTE));
        assertEquals(1, handebol.pontosPara(new Placar(20, 20), Lado.VISITANTE));
    }

    @Test
    @DisplayName("RN-08: no handebol, gols sofridos vêm antes de gols marcados (o contrário do futsal)")
    void mesmosPlacaresOrdemDiferenteDoFutsal() {
        Chave noHandebol = new Chave("Chave A", handebol, Arrays.asList(a, b, c));
        Chave noFutsal = new Chave("Chave A", new Futsal(), Arrays.asList(a, b, c));
        for (Chave chave : Arrays.asList(noHandebol, noFutsal)) {
            chave.registrar(jogo(a, 2, 2, b));
            chave.registrar(jogo(a, 5, 3, c));
            chave.registrar(jogo(b, 3, 1, c));
        }

        List<Posicao> handebolFinal = classificador.classificar(noHandebol, handebol);
        List<Posicao> futsalFinal = classificador.classificar(noFutsal, new Futsal());

        // Saldo igual (+2). A marcou 7 e sofreu 5. B marcou 5 e sofreu 3.
        assertEquals(Arrays.asList("B", "A", "C"), ordem(handebolFinal));
        assertEquals("Gols sofridos", handebolFinal.get(0).getCriterioDecisivo());
        assertEquals(Arrays.asList("A", "B", "C"), ordem(futsalFinal));
        assertEquals("Gols marcados", futsalFinal.get(0).getCriterioDecisivo());
    }

    @Test
    @DisplayName("RN-09: entre três, vale a tabela só com os jogos entre as empatadas (Art. 67, § 2º)")
    void empateTriploPeloSaldoNoConfrontoDireto() {
        Chave chave = new Chave("Chave B", handebol, Arrays.asList(a, b, c, d));
        // Jogos entre as três empatadas: cada uma ganha um e perde um.
        chave.registrar(jogo(a, 25, 20, b));
        chave.registrar(jogo(b, 30, 20, c));
        chave.registrar(jogo(c, 22, 21, a));
        // As três vencem D, com placares bem diferentes.
        chave.registrar(jogo(a, 40, 10, d));
        chave.registrar(jogo(b, 21, 20, d));
        chave.registrar(jogo(c, 35, 10, d));

        List<Posicao> classificacao = classificador.classificar(chave, handebol);

        // Saldo só entre as três: B +5, A +4, C -9.
        // Saldo em toda a fase daria outra ordem: A +34, C +16, B +6.
        assertEquals(Arrays.asList("B", "A", "C", "D"), ordem(classificacao));
        assertEquals(Arrays.asList("Saldo de gols no confronto direto", "Saldo de gols no confronto direto",
                "Saldo de gols no confronto direto", "Pontos"), criterios(classificacao));
    }

    @Test
    @DisplayName("RN-08, RN-10: tudo igual nos gols, decide a menor pontuação de sanções")
    void decideNasSancoes() {
        Chave chave = new Chave("Chave C", handebol, Arrays.asList(a, b, c));
        chave.registrar(jogo(a, 20, 20, b, Disciplina.sancoes(5, 2)));
        chave.registrar(jogo(a, 25, 20, c));
        chave.registrar(jogo(b, 25, 20, c));

        List<Posicao> classificacao = classificador.classificar(chave, handebol);

        assertEquals(Arrays.asList("B", "A", "C"), ordem(classificacao));
        assertEquals("Sanções", classificacao.get(0).getCriterioDecisivo());
    }
}
