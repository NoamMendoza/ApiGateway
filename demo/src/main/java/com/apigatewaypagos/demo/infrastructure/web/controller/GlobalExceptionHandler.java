package com.apigatewaypagos.demo.infrastructure.web.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.apigatewaypagos.demo.domain.exception.InvalidPaymentStateException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Datos invalidos", "detalle", ex.getMessage()));
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPaymentState(InvalidPaymentStateException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Operacion no permitida", "detalle", ex.getMessage()));
    }

    @ExceptionHandler(com.apigatewaypagos.demo.domain.exception.PaymentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePaymentNotFound(com.apigatewaypagos.demo.domain.exception.PaymentNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Pago no encontrado", "detalle", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Formato invalido", "detalle", "El valor proporcionado '" + ex.getValue() + "' no es valido para " + ex.getName()));
    }
}
