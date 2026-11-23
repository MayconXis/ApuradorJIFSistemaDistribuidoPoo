package br.edu.ifg.apurador.persistencia.domain;

import br.edu.ifg.apurador.cluster.apuracao.LinhaDaTabela;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posicao")
@IdClass(PosicaoId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PosicaoEntidade {

    @Id
    @Column(name = "chave_id", length = 40)
    private String chaveId;

    @Id
    @Column(name = "colocacao")
    private int colocacao;

    @Column(name = "equipe", nullable = false, length = 60)
    private String equipe;

    @Column(name = "pontos", nullable = false)
    private int pontos;

    @Column(name = "jogos", nullable = false)
    private int jogos;

    @Column(name = "vitorias", nullable = false)
    private int vitorias;

    @Column(name = "empates", nullable = false)
    private int empates;

    @Column(name = "derrotas", nullable = false)
    private int derrotas;

    @Column(name = "pro", nullable = false)
    private int pro;

    @Column(name = "contra", nullable = false)
    private int contra;

    @Column(name = "criterio_decisivo", nullable = false, length = 80)
    private String criterioDecisivo;

    @Column(name = "pendente_de_sorteio", nullable = false)
    private boolean pendenteDeSorteio;

    public PosicaoEntidade(String chaveId, LinhaDaTabela linha) {
        this.chaveId = chaveId;
        this.colocacao = linha.getColocacao();
        this.equipe = linha.getEquipe();
        this.pontos = linha.getPontos();
        this.jogos = linha.getJogos();
        this.vitorias = linha.getVitorias();
        this.empates = linha.getEmpates();
        this.derrotas = linha.getDerrotas();
        this.pro = linha.getPro();
        this.contra = linha.getContra();
        this.criterioDecisivo = linha.getCriterioDecisivo();
        this.pendenteDeSorteio = linha.isPendenteDeSorteio();
    }

    public LinhaDaTabela paraLinha() {
        return new LinhaDaTabela(colocacao, equipe, pontos, jogos, vitorias, empates, derrotas, pro, contra,
                criterioDecisivo, pendenteDeSorteio);
    }
}
