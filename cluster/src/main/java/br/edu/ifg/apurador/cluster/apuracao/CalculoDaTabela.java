package br.edu.ifg.apurador.cluster.apuracao;

import br.edu.ifg.apurador.core.desempate.Estatisticas;
import br.edu.ifg.apurador.core.modalidade.Modalidade;
import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Partida;
import br.edu.ifg.apurador.core.tabela.Campanha;
import br.edu.ifg.apurador.core.tabela.Chave;
import br.edu.ifg.apurador.core.tabela.Classificador;
import br.edu.ifg.apurador.core.tabela.Posicao;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ponte entre o histórico e o core: monta a Chave do core com as partidas que
 * têm resultado e pede a classificação ao Classificador.
 *
 * É uma função pura dos dados recebidos: as mesmas partidas dão sempre a mesma
 * tabela. É isso que torna o reprocessamento seguro. O worker usa esta classe
 * para apurar e o Gateway usa a mesma classe para auditar.
 */
public final class CalculoDaTabela {

    private final Classificador classificador = new Classificador();

    public List<LinhaDaTabela> calcular(DadosDaChave dados, Collection<PartidaGravada> partidas, Modalidade modalidade) {
        Map<String, Equipe> equipes = new LinkedHashMap<>();
        for (String nome : dados.getEquipes()) {
            equipes.put(nome, new Equipe(nome));
        }
        Chave chave = new Chave(dados.getNome(), modalidade, equipes.values());
        for (PartidaGravada gravada : partidas) {
            if (!gravada.temResultado()) {
                continue;
            }
            EventoDeResultado resultado = gravada.getResultado();
            Equipe mandante = equipes.get(gravada.getMandante());
            Equipe visitante = equipes.get(gravada.getVisitante());
            if (resultado.isWo()) {
                chave.registrar(Partida.porWO(gravada.getId(), mandante, visitante, resultado.getAusente()));
            } else {
                chave.registrar(Partida.jogada(gravada.getId(), mandante, visitante, resultado.placar(), resultado.disciplina()));
            }
        }
        List<Partida> efetivas = chave.partidasEfetivas();
        List<LinhaDaTabela> linhas = new ArrayList<>();
        for (Posicao posicao : classificador.classificar(chave, modalidade)) {
            Equipe equipe = posicao.getEquipe();
            Campanha campanha = chave.campanhaDe(equipe);
            linhas.add(new LinhaDaTabela(posicao.getColocacao(), equipe.getNome(), posicao.getPontos(),
                    campanha.getJogos(), campanha.getVitorias(), campanha.getEmpates(), campanha.getDerrotas(),
                    Estatisticas.marcados(equipe, efetivas), Estatisticas.sofridos(equipe, efetivas),
                    posicao.getCriterioDecisivo(), posicao.isPendenteDeSorteio()));
        }
        return linhas;
    }
}
