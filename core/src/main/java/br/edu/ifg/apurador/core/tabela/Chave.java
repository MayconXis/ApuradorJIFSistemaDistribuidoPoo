package br.edu.ifg.apurador.core.tabela;

import br.edu.ifg.apurador.core.modalidade.Modalidade;
import br.edu.ifg.apurador.core.sumula.Equipe;
import br.edu.ifg.apurador.core.sumula.Lado;
import br.edu.ifg.apurador.core.sumula.Partida;
import br.edu.ifg.apurador.core.sumula.Placar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Uma chave da fase classificatória: equipes que jogam entre si em rodízio simples
 * (Arts. 28 e 29). Guarda as partidas em um ArrayList e uma Campanha por Equipe
 * em um Map.
 */
public class Chave {

    private final String nome;
    private final Modalidade modalidade;
    private final Map<Equipe, Campanha> campanhas = new LinkedHashMap<>();
    private final List<Partida> partidas = new ArrayList<>();

    public Chave(String nome, Modalidade modalidade, Collection<Equipe> equipes) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("A chave precisa de um nome.");
        }
        if (modalidade == null) {
            throw new IllegalArgumentException("A chave precisa de uma modalidade.");
        }
        if (equipes == null || equipes.size() < 2) {
            throw new IllegalArgumentException("A chave precisa de pelo menos duas equipes.");
        }
        this.nome = nome.trim();
        this.modalidade = modalidade;
        for (Equipe equipe : equipes) {
            if (campanhas.containsKey(equipe)) {
                throw new IllegalArgumentException("Equipe repetida na chave: " + equipe);
            }
            campanhas.put(equipe, new Campanha(equipe));
        }
    }

    /**
     * Registra uma partida e atualiza a campanha das duas equipes.
     * Em rodízio simples, cada par de equipes joga uma vez só.
     */
    public void registrar(Partida partida) {
        if (!campanhas.containsKey(partida.getMandante()) || !campanhas.containsKey(partida.getVisitante())) {
            throw new IllegalArgumentException("A partida " + partida.getId() + " tem equipe que não é da chave.");
        }
        for (Partida outra : partidas) {
            if (outra.getId().equals(partida.getId())) {
                throw new IllegalArgumentException("Partida repetida: " + partida.getId());
            }
            if (outra.envolve(partida.getMandante()) && outra.envolve(partida.getVisitante())) {
                throw new IllegalArgumentException("Em rodízio simples, " + partida.getMandante() + " e "
                        + partida.getVisitante() + " jogam uma vez só.");
            }
        }
        if (!partida.isWo()) {
            modalidade.validar(partida.getPlacar());
        }
        Placar efetivo = placarEfetivo(partida);
        partidas.add(partida);
        for (Lado lado : Lado.values()) {
            int pontos = modalidade.pontosPara(efetivo, lado);
            int resultado = Integer.compare(efetivo.de(lado), efetivo.contra(lado));
            campanhas.get(partida.equipeDo(lado)).registrar(pontos, resultado);
        }
    }

    /**
     * Sobrecarga para o WO: recebe só quem venceu e quem não compareceu.
     * O placar é calculado pela modalidade.
     */
    public void registrar(String idDaPartida, Equipe vencedor, Equipe ausente) {
        registrar(Partida.porWO(idDaPartida, vencedor, ausente, Lado.VISITANTE));
    }

    /**
     * Partidas com o placar que vale para a classificação. A partida por WO sai
     * com o placar calculado agora pela modalidade. No futsal e no handebol esse
     * placar depende dos outros jogos da chave, então é recalculado a cada chamada.
     */
    public List<Partida> partidasEfetivas() {
        List<Partida> efetivas = new ArrayList<>();
        for (Partida partida : partidas) {
            efetivas.add(partida.isWo() ? partida.comPlacar(placarEfetivo(partida)) : partida);
        }
        return efetivas;
    }

    private Placar placarEfetivo(Partida partida) {
        return partida.isWo() ? modalidade.placarDeWO(partida.getAusente(), partidas) : partida.getPlacar();
    }

    public Campanha campanhaDe(Equipe equipe) {
        Campanha campanha = campanhas.get(equipe);
        if (campanha == null) {
            throw new IllegalArgumentException(equipe + " não é desta chave.");
        }
        return campanha;
    }

    /** Lista só de leitura: quem recebe não consegue incluir nem remover partidas. */
    public List<Partida> getPartidas() {
        return Collections.unmodifiableList(partidas);
    }

    public Collection<Equipe> getEquipes() {
        return Collections.unmodifiableCollection(campanhas.keySet());
    }

    public String getNome() {
        return nome;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }
}
