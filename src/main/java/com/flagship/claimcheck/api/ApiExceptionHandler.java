package com.flagship.claimcheck.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

import static com.flagship.claimcheck.service.AdjudicationService.RULE_SET_VERSION;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> invalid(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.put(e.getField(), e.getDefaultMessage()));
        return Map.of("status", 400, "error", "Validation failed", "ruleSetVersion", RULE_SET_VERSION,
            "timestamp", Instant.now(), "fields", fields);
    }
}
