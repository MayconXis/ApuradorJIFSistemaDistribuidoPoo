package br.edu.ifg.apurador.persistencia.domain;

import br.edu.ifg.apurador.cluster.apuracao.EstadoDoEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "evento_resultado")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoEntidade extends DadosDoResultado {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evento_seq")
    @SequenceGenerator(name = "evento_seq", sequenceName = "evento_seq", allocationSize = 1)
    @Column(name = "seq")
    private Long seq;

    @Column(name = "evento_id", nullable = false, unique = true, length = 60)
    private String eventoId;

    @Column(name = "chave_id", nullable = false, length = 40)
    private String chaveId;

    @Column(name = "partida_id", nullable = false, length = 60)
    private String partidaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoDoEvento estado;

    @Column(name = "motivo", length = 300)
    private String motivo;

    @Column(name = "worker", length = 60)
    private String worker;

    @Column(name = "recebido_em", nullable = false)
    private Instant recebidoEm;

    @Column(name = "encerrado_em")
    private Instant encerradoEm;

    @Column(name = "aplicacoes", nullable = false)
    private int aplicacoes;

    public EventoEntidade(String eventoId, String chaveId, String partidaId) {
        this.eventoId = eventoId;
        this.chaveId = chaveId;
        this.partidaId = partidaId;
        this.estado = EstadoDoEvento.PENDENTE;
        this.recebidoEm = Instant.now();
    }

    public void marcarAplicado(String porWorker) {
        this.estado = EstadoDoEvento.APLICADO;
        this.worker = porWorker;
        this.encerradoEm = Instant.now();
        this.aplicacoes++;
    }

    public void encerrar(EstadoDoEvento novoEstado, String porque, String porWorker) {
        this.estado = novoEstado;
        this.motivo = porque != null && porque.length() > 300 ? porque.substring(0, 300) : porque;
        this.worker = porWorker;
        this.encerradoEm = Instant.now();
    }
}
