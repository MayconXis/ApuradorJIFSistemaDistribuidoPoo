package br.edu.ifg.apurador.dispatcher.handler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import br.edu.ifg.apurador.cluster.despacho.ResultadoInvalidoException;
import br.edu.ifg.apurador.dispatcher.dto.response.ErroResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String mensagem = campos.isEmpty()
                ? "Dados inválidos na requisição."
                : campos.values().iterator().next();
        return responder(BAD_REQUEST, mensagem, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleCorpoInvalido(HttpMessageNotReadableException e) {
        return responder(BAD_REQUEST, "Corpo da requisição inválido ou malformado.", null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> handleResponseStatus(ResponseStatusException e) {
        return responder(e.getStatusCode(), e.getReason(), null);
    }

    @ExceptionHandler(ResultadoInvalidoException.class)
    public ResponseEntity<ErroResponse> handleResultadoInvalido(ResultadoInvalidoException e) {
        return responder(UNPROCESSABLE_ENTITY, e.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleArgumentoInvalido(IllegalArgumentException e) {
        return responder(BAD_REQUEST, e.getMessage(), null);
    }

    private ResponseEntity<ErroResponse> responder(HttpStatusCode status, String mensagem, Map<String, String> campos) {
        ErroResponse body = ErroResponse.builder()
                .status(status.value())
                .erro(mensagem)
                .timestamp(Instant.now())
                .camposInvalidos(campos == null || campos.isEmpty() ? null : campos)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
