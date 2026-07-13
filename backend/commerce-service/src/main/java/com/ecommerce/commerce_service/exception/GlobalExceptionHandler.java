package com.ecommerce.commerce_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> business(
            BusinessException ex,
            HttpServletRequest request
    ) {
        return response(
                ex.getStatus(),
                ex.getErrorCode(),
                ex.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> optimistic(
            OptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Modificación concurrente correlationId={} path={}",
                MDC.get("correlationId"),
                request.getRequestURI()
        );

        return response(
                HttpStatus.CONFLICT,
                ErrorCode.CONCURRENT_MODIFICATION,
                "El recurso fue modificado por otra operación. Recargue e intente nuevamente",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError :
                ex.getBindingResult().getFieldErrors()) {

            errors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        return response(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Existen campos inválidos",
                request,
                errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> constraint(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(
                violation -> errors.putIfAbsent(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                )
        );

        return response(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Existen parámetros inválidos",
                request,
                errors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error(
                "Error no controlado correlationId={} path={}",
                MDC.get("correlationId"),
                request.getRequestURI(),
                ex
        );

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "Ocurrió un error interno",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            ErrorCode code,
            String message,
            HttpServletRequest request,
            Map<String, String> errors
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code.name(),
                message,
                request.getRequestURI(),
                MDC.get("correlationId"),
                errors
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }
}