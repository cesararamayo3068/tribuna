package com.tribuna.analysis;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(InvalidCsvException.class)
    public ResponseEntity<Map<String, String>> invalid(InvalidCsvException e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge() { return ResponseEntity.status(413).body(Map.of("message", "El archivo supera el límite de 5 MB.")); }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() == null ? "No se pudo completar la solicitud." : e.getReason()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception e) {
        LOG.error("Error al procesar la solicitud", e);
        return ResponseEntity.internalServerError().body(Map.of("message", "No pudimos completar el análisis. Intentá de nuevo en unos instantes."));
    }
}
