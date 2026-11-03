package com.tribuna.tournament;

import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages="com.tribuna.tournament")
@Order(-10)
public class TournamentInputExceptionHandler {
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String,String>> malformed(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of("message","Revisá los datos enviados: hay un campo con formato inválido."));
    }
}
