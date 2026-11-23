package br.edu.ifg.apurador.cluster.despacho;

/** O resultado enviado não passou na validação. A mensagem traz o motivo. */
public class ResultadoInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResultadoInvalidoException(String motivo) {
        super(motivo);
    }
}
