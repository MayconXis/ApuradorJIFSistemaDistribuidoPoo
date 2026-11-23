package br.edu.ifg.apurador.core;

import static br.edu.ifg.apurador.core.Apoio.criterios;
import static br.edu.ifg.apurador.core.Apoio.jogo;
import static br.edu.ifg.apurador.core.Apoio.ordem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.edu.ifg.apurador.core.modalidade.Futsal;
import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.tabela.Chave;
import br.edu.ifg.apurador.core.tabela.Classificador;
import br.edu.ifg.apurador.core.tabela.Posicao;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TU-01. Teste obrigatório do domínio: o empate triplo na Chave A do futsal.
 * Requisitos: RF-05, RF-06, RF-07. Regras: RN-03, RN-05, RN-06. Suposição: SUP-01.
 */
@DisplayName("TU-01 Empate triplo no futsal (RF-05, RF-06, RF-07, SUP-01)")
class FutsalEmpateTriploTest {

    private final Equipe ifce = new Equipe("IFCE");
    private final Equipe ifg = new Equipe("IFG");
    private final Equipe ifsp = new Equipe("IFSP");

    private Chave chaveA() {
        Chave chave = new Chave("Chave A", new Futsal(), Arrays.asList(ifce, ifg, ifsp));
        chave.registrar(jogo(ifce, 4, 1, ifg));
        chave.registrar(jogo(ifg, 3, 1, ifsp));
        chave.registrar(jogo(ifsp, 4, 3, ifce));
        return chave;
    }

    @Test
    @DisplayName("As três equipes terminam com 3 pontos")
    void todasComTresPontos() {
        Chave chave = chaveA();
        assertEquals(3, chave.campanhaDe(ifce).getPontos());
        assertEquals(3, chave.campanhaDe(ifg).getPontos());
        assertEquals(3, chave.campanhaDe(ifsp).getPontos());
    }

    @Test
    @DisplayName("Leitura adotada: IFCE em 1º por saldo, IFG em 2º por confronto direto, IFSP em 3º")
    void leituraAdotadaVoltaAoConfrontoDireto() {
        List<Posicao> classificacao = new Classificador().classificar(chaveA(), new Futsal());

        assertEquals(Arrays.asList("IFCE", "IFG", "IFSP"), ordem(classificacao));
        assertEquals(Arrays.asList("Saldo de gols", "Confronto direto", "Confronto direto"), criterios(classificacao));
        assertEquals(1, classificacao.get(0).getColocacao());
        assertEquals(2, classificacao.get(1).getColocacao());
        assertEquals(3, classificacao.get(2).getColocacao());
        for (Posicao posicao : classificacao) {
            assertFalse(posicao.isPendenteDeSorteio());
        }
    }

    @Test
    @DisplayName("Outra leitura (seguir o § 2º): IFSP ficaria em 2º por gols marcados")
    void outraLeituraDariaOSegundoLugarAoIfsp() {
        Classificador semVoltar = new Classificador() {
            @Override
            protected boolean recomecaQuandoOEmpateDiminui() {
                return false;
            }
        };

        List<Posicao> classificacao = semVoltar.classificar(chaveA(), new Futsal());

        assertEquals(Arrays.asList("IFCE", "IFSP", "IFG"), ordem(classificacao));
        assertEquals(Arrays.asList("Saldo de gols", "Gols marcados", "Gols marcados"), criterios(classificacao));
    }

    @Test
    @DisplayName("A ordem em que os jogos são registrados não muda a classificação")
    void ordemDeRegistroNaoImporta() {
        Chave invertida = new Chave("Chave A", new Futsal(), Arrays.asList(ifsp, ifg, ifce));
        invertida.registrar(jogo(ifsp, 4, 3, ifce));
        invertida.registrar(jogo(ifg, 3, 1, ifsp));
        invertida.registrar(jogo(ifce, 4, 1, ifg));

        Classificador classificador = new Classificador();
        assertEquals(classificador.classificar(chaveA(), new Futsal()),
                classificador.classificar(invertida, new Futsal()));
    }
}
