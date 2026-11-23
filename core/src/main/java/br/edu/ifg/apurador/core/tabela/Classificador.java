package br.edu.ifg.apurador.core.tabela;

import br.edu.ifg.apurador.core.desempate.CriterioDesempate;
import br.edu.ifg.apurador.core.modalidade.Modalidade;
import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Calcula a classificação de uma chave.
 *
 * O Classificador não conhece nenhuma modalidade pelo nome: ele pede à Modalidade
 * a lista de critérios e trabalha só com a interface CriterioDesempate. Por isso
 * uma modalidade nova não exige mudança nesta classe.
 *
 * Passos:
 * 1. Ordena as equipes por pontos.
 * 2. Para cada grupo de equipes com os mesmos pontos, percorre os critérios na
 *    ordem do regulamento, usando só os que valem para aquela quantidade de equipes.
 * 3. O primeiro critério que separa alguém divide o grupo em blocos. Bloco de uma
 *    equipe está resolvido. Bloco com duas ou mais continua empatado e é
 *    desempatado de novo.
 * 4. Se nenhum critério separa, as posições ficam pendentes de sorteio.
 */
public class Classificador {

    /** Critério decisivo de quem não empatou em pontos com ninguém. */
    public static final String POR_PONTOS = "Pontos";

    /** Classifica a chave pela modalidade dela. */
    public List<Posicao> classificar(Chave chave) {
        return classificar(chave, chave.getModalidade());
    }

    /**
     * Classifica a chave pelas regras da modalidade informada.
     * A modalidade tem de ser a mesma da chave, porque os pontos das campanhas
     * foram calculados por ela.
     */
    public List<Posicao> classificar(Chave chave, Modalidade modalidade) {
        if (!modalidade.equals(chave.getModalidade())) {
            throw new IllegalArgumentException("A chave " + chave.getNome() + " é de " + chave.getModalidade()
                    + " e não pode ser classificada como " + modalidade + ".");
        }
        List<Partida> jogos = chave.partidasEfetivas();
        List<CriterioDesempate> criterios = modalidade.criterios();

        List<Equipe> equipes = new ArrayList<>(chave.getEquipes());
        equipes.sort(Comparator.comparingInt((Equipe e) -> chave.campanhaDe(e).getPontos()).reversed());

        List<Posicao> classificacao = new ArrayList<>();
        int inicio = 0;
        while (inicio < equipes.size()) {
            int pontos = chave.campanhaDe(equipes.get(inicio)).getPontos();
            int fim = inicio;
            List<Equipe> grupo = new ArrayList<>();
            while (fim < equipes.size() && chave.campanhaDe(equipes.get(fim)).getPontos() == pontos) {
                grupo.add(equipes.get(fim));
                fim++;
            }
            if (grupo.size() == 1) {
                classificacao.add(new Posicao(classificacao.size() + 1, grupo.get(0), pontos, POR_PONTOS, false));
            } else {
                desempatar(grupo, pontos, jogos, criterios, 0, grupo.size(), classificacao);
            }
            inicio = fim;
        }
        return Collections.unmodifiableList(classificacao);
    }

    /**
     * SUPOSIÇÃO SUP-01, isolada neste método.
     *
     * Quando um empate entre três equipes se reduz a duas, o regulamento não diz
     * se a lista de critérios recomeça. Com true (leitura adotada), o grupo menor
     * volta ao início da lista, e por isso o confronto direto passa a valer para
     * as duas que sobraram. Com false, o grupo menor segue do critério seguinte,
     * ainda pela lista de três ou mais.
     *
     * É protected para que a outra leitura possa ser testada em uma subclasse.
     */
    protected boolean recomecaQuandoOEmpateDiminui() {
        return true;
    }

    private void desempatar(List<Equipe> grupo, int pontos, List<Partida> jogos, List<CriterioDesempate> criterios,
                            int primeiroCriterio, int qtdParaAplicar, List<Posicao> classificacao) {
        String ultimoCriterio = "Sorteio";
        for (int i = primeiroCriterio; i < criterios.size(); i++) {
            CriterioDesempate criterio = criterios.get(i);
            if (!criterio.aplicavelA(qtdParaAplicar)) {
                continue;
            }
            ultimoCriterio = criterio.nome();
            Collection<Partida> visiveis = criterio.restritoAosEmpatados() ? jogosEntre(grupo, jogos) : jogos;
            List<List<Equipe>> blocos = separar(grupo, criterio, visiveis);
            if (blocos.size() == 1) {
                continue;
            }
            for (List<Equipe> bloco : blocos) {
                if (bloco.size() == 1) {
                    classificacao.add(new Posicao(classificacao.size() + 1, bloco.get(0), pontos,
                            criterio.nome(), false));
                } else if (recomecaQuandoOEmpateDiminui()) {
                    desempatar(bloco, pontos, jogos, criterios, 0, bloco.size(), classificacao);
                } else {
                    desempatar(bloco, pontos, jogos, criterios, i + 1, qtdParaAplicar, classificacao);
                }
            }
            return;
        }
        // Nenhum critério separou as equipes: a ordem depende de sorteio (SUP-06).
        // Para o resultado ser sempre o mesmo, a ordem provisória é a alfabética.
        List<Equipe> provisoria = new ArrayList<>(grupo);
        provisoria.sort(Comparator.comparing(Equipe::getNome));
        for (Equipe equipe : provisoria) {
            classificacao.add(new Posicao(classificacao.size() + 1, equipe, pontos, ultimoCriterio, true));
        }
    }

    /** Ordena o grupo pelo critério e corta a lista onde o critério distingue duas equipes vizinhas. */
    private List<List<Equipe>> separar(List<Equipe> grupo, CriterioDesempate criterio, Collection<Partida> jogos) {
        List<Equipe> ordenado = new ArrayList<>(grupo);
        ordenado.sort((a, b) -> criterio.comparar(a, b, jogos));
        List<List<Equipe>> blocos = new ArrayList<>();
        List<Equipe> atual = new ArrayList<>();
        for (Equipe equipe : ordenado) {
            if (!atual.isEmpty() && criterio.comparar(atual.get(atual.size() - 1), equipe, jogos) != 0) {
                blocos.add(atual);
                atual = new ArrayList<>();
            }
            atual.add(equipe);
        }
        blocos.add(atual);
        return blocos;
    }

    /** Só os jogos em que as duas equipes pertencem ao grupo empatado. */
    private List<Partida> jogosEntre(List<Equipe> grupo, List<Partida> jogos) {
        List<Partida> entreElas = new ArrayList<>();
        for (Partida partida : jogos) {
            if (grupo.contains(partida.getMandante()) && grupo.contains(partida.getVisitante())) {
                entreElas.add(partida);
            }
        }
        return entreElas;
    }
}
