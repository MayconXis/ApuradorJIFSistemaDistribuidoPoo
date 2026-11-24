package br.edu.ifg.apurador.dispatcher.handler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import br.edu.ifg.apurador.cluster.despacho.ResultadoInvalidoException;
import br.edu.ifg.apurador.dispatcher.dto.response.ErroResponse;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ErroResponse(e.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleCamposInvalidos(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
        return ResponseEntity.status(BAD_REQUEST).body(new ErroResponse(mensagem));
    }

    @ExceptionHandler(ResultadoInvalidoException.class)
    public ResponseEntity<ErroResponse> handleResultadoInvalido(ResultadoInvalidoException e) {
        return ResponseEntity.status(UNPROCESSABLE_ENTITY).body(new ErroResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.status(BAD_REQUEST).body(new ErroResponse(e.getMessage()));
    }
}
