package br.edu.ifg.apurador.cluster.despacho;

import br.edu.ifg.apurador.cluster.apuracao.DadosDaChave;
import br.edu.ifg.apurador.cluster.apuracao.EventoDeResultado;
import br.edu.ifg.apurador.cluster.apuracao.PartidaGravada;
import br.edu.ifg.apurador.cluster.apuracao.ValidadorDeResultado;
import br.edu.ifg.apurador.core.modalidade.CatalogoDeModalidades;
import br.edu.ifg.apurador.core.modalidade.Modalidade;
import br.edu.ifg.apurador.core.sumula.Lado;

/**
 * Caso de uso do Gateway: registrar um resultado.
 *
 * 1. Valida pela regra da modalidade (core).
 * 2. Grava o evento como PENDENTE no histórico. A partir daqui ele não se perde.
 * 3. Pede ao Coordenador que avise o worker dono da chave.
 *
 * A ordem importa: grava antes de publicar. Se a publicação falhar ou o Gateway
 * cair entre os passos 2 e 3, o evento já está no banco e a varredura de
 * pendentes o entrega depois.
 */
public class RegistradorDeResultados {

    private final RegistroDeEventos registro;
    private final Coordenador coordenador;
    private final CatalogoDeModalidades catalogo;
    private final ValidadorDeResultado validador = new ValidadorDeResultado();

    public RegistradorDeResultados(RegistroDeEventos registro, Coordenador coordenador, CatalogoDeModalidades catalogo) {
        this.registro = registro;
        this.coordenador = coordenador;
        this.catalogo = catalogo;
    }

    /** @throws ResultadoInvalidoException se o resultado não passa na validação */
    public Recibo registrar(EventoDeResultado enviado) {
        PartidaGravada partida = registro.partida(enviado.getPartidaId());
        if (partida == null) {
            throw new ResultadoInvalidoException("A partida " + enviado.getPartidaId() + " não existe.");
        }
        DadosDaChave chave = registro.chave(partida.getChaveId());
        Modalidade modalidade = catalogo.buscar(chave.getModalidade());
        EventoDeResultado evento = new EventoDeResultado(enviado.getEventoId(), 0, partida.getChaveId(), partida.getId(),
                enviado.getPlacarMandante(), enviado.getPlacarVisitante(), enviado.getParciais(), enviado.isWo(),
                enviado.getAusente(),
                new int[] {enviado.amarelos(Lado.MANDANTE), enviado.amarelos(Lado.VISITANTE)},
                new int[] {enviado.vermelhos(Lado.MANDANTE), enviado.vermelhos(Lado.VISITANTE)},
                new int[] {enviado.sancoes(Lado.MANDANTE), enviado.sancoes(Lado.VISITANTE)});
        String motivo = validador.validar(evento, partida, modalidade);
        if (motivo != null) {
            throw new ResultadoInvalidoException(motivo);
        }
        Recibo recibo = registro.gravarPendente(evento);
        String worker = coordenador.despachar(partida.getChaveId(), Tarefa.NOVO_EVENTO);
        return recibo.comWorker(worker);
    }
}
