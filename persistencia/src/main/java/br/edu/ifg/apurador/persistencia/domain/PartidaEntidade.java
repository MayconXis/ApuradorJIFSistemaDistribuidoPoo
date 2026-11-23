package br.edu.ifg.apurador.persistencia.domain;

import br.edu.ifg.apurador.cluster.apuracao.EstadoDaPartida;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "partida")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartidaEntidade extends DadosDoResultado {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "chave_id", nullable = false, length = 40)
    private String chaveId;

    @Column(name = "mandante", nullable = false, length = 60)
    private String mandante;

    @Column(name = "visitante", nullable = false, length = 60)
    private String visitante;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoDaPartida estado;

    @Column(name = "evento_id", length = 60)
    private String eventoId;

    @Column(name = "seq_aplicada", nullable = false)
    private long seqAplicada;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    public PartidaEntidade(String id, String chaveId, String mandante, String visitante) {
        this.id = id;
        this.chaveId = chaveId;
        this.mandante = mandante;
        this.visitante = visitante;
        this.estado = EstadoDaPartida.AGENDADA;
        this.atualizadaEm = Instant.now();
    }

    public void receberResultado(EventoEntidade evento, EstadoDaPartida novoEstado) {
        copiarResultadoDe(evento);
        this.eventoId = evento.getEventoId();
        this.seqAplicada = evento.getSeq();
        this.estado = novoEstado;
        this.atualizadaEm = Instant.now();
    }
}
