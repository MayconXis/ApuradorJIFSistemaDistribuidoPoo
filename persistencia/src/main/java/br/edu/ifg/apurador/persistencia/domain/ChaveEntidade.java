package br.edu.ifg.apurador.persistencia.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chave")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChaveEntidade {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @Column(name = "nome", nullable = false, length = 60)
    private String nome;

    @Column(name = "modalidade", nullable = false, length = 20)
    private String modalidade;

    @Column(name = "versao", nullable = false)
    private long versao;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chave_equipe", joinColumns = @JoinColumn(name = "chave_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "equipe", nullable = false, length = 60)
    private List<String> equipes = new ArrayList<>();

    public ChaveEntidade(String id, String nome, String modalidade, List<String> equipes) {
        this.id = id;
        this.nome = nome;
        this.modalidade = modalidade;
        this.equipes = new ArrayList<>(equipes);
        this.criadaEm = Instant.now();
        this.atualizadaEm = this.criadaEm;
    }

    public void avancarVersao() {
        versao++;
        atualizadaEm = Instant.now();
    }
}
