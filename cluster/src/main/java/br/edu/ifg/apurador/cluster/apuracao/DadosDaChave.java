package br.edu.ifg.apurador.cluster.apuracao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Cadastro de uma chave como está no histórico: identificador, nome, modalidade e equipes. Imutável. */
public final class DadosDaChave {

    private final String id;
    private final String nome;
    private final String modalidade;
    private final List<String> equipes;

    public DadosDaChave(String id, String nome, String modalidade, List<String> equipes) {
        this.id = id;
        this.nome = nome;
        this.modalidade = modalidade;
        this.equipes = Collections.unmodifiableList(new ArrayList<>(equipes));
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    /** Código da modalidade no catálogo, por exemplo FUTSAL. */
    public String getModalidade() {
        return modalidade;
    }

    public List<String> getEquipes() {
        return equipes;
    }
}
