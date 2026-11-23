package br.edu.ifg.apurador.persistencia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class DadosDoResultado {

    @Column(name = "placar_mandante", nullable = false)
    private int placarMandante;

    @Column(name = "placar_visitante", nullable = false)
    private int placarVisitante;

    @Column(name = "parciais", length = 120)
    private String parciais;

    @Column(name = "wo", nullable = false)
    private boolean wo;

    @Column(name = "ausente", length = 10)
    private String ausente;

    @Column(name = "amarelos_mandante", nullable = false)
    private int amarelosMandante;

    @Column(name = "amarelos_visitante", nullable = false)
    private int amarelosVisitante;

    @Column(name = "vermelhos_mandante", nullable = false)
    private int vermelhosMandante;

    @Column(name = "vermelhos_visitante", nullable = false)
    private int vermelhosVisitante;

    @Column(name = "sancoes_mandante", nullable = false)
    private int sancoesMandante;

    @Column(name = "sancoes_visitante", nullable = false)
    private int sancoesVisitante;

    public void copiarResultadoDe(DadosDoResultado outro) {
        this.placarMandante = outro.placarMandante;
        this.placarVisitante = outro.placarVisitante;
        this.parciais = outro.parciais;
        this.wo = outro.wo;
        this.ausente = outro.ausente;
        this.amarelosMandante = outro.amarelosMandante;
        this.amarelosVisitante = outro.amarelosVisitante;
        this.vermelhosMandante = outro.vermelhosMandante;
        this.vermelhosVisitante = outro.vermelhosVisitante;
        this.sancoesMandante = outro.sancoesMandante;
        this.sancoesVisitante = outro.sancoesVisitante;
    }
}
