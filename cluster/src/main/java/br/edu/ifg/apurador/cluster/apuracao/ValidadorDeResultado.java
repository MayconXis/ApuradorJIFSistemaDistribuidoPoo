package br.edu.ifg.apurador.cluster.apuracao;

import br.edu.ifg.apurador.core.modalidade.Modalidade;

/**
 * Confere um evento de resultado contra a partida e a regra da modalidade.
 * O Gateway usa antes de aceitar o envio (RF-03). O worker usa de novo antes de
 * aplicar, como segunda barreira.
 */
public final class ValidadorDeResultado {

    /** @return null se o evento é válido, ou o motivo da recusa */
    public String validar(EventoDeResultado evento, PartidaGravada partida, Modalidade modalidade) {
        if (partida == null) {
            return "A partida " + evento.getPartidaId() + " não existe.";
        }
        if (evento.getChaveId() != null && !evento.getChaveId().equals(partida.getChaveId())) {
            return "A partida " + partida.getId() + " não é da chave " + evento.getChaveId() + ".";
        }
        try {
            evento.disciplina();
            if (evento.isWo()) {
                if (evento.getAusente() == null) {
                    return "No WO, informe qual lado não compareceu.";
                }
                return null;
            }
            if (evento.getPlacarMandante() < 0 || evento.getPlacarVisitante() < 0) {
                return "Placar não pode ser negativo.";
            }
            modalidade.validar(evento.placar());
            return null;
        } catch (IllegalArgumentException invalido) {
            return invalido.getMessage();
        }
    }
}
