package com.ecommerce.auth_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
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

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {

        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        exception.getStatus().value(),
                        exception.getStatus().getReasonPhrase(),
                        exception.getErrorCode().name(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        MDC.get("correlationId"),
                        Map.of()
                );

        return ResponseEntity
                .status(exception.getStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        for (
                FieldError fieldError :
                exception.getBindingResult().getFieldErrors()
        ) {
            validationErrors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        ErrorCode.VALIDATION_ERROR.name(),
                        "Existen campos inválidos",
                        request.getRequestURI(),
                        MDC.get("correlationId"),
                        validationErrors
                );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        exception.getConstraintViolations()
                .forEach(violation ->
                        validationErrors.put(
                                violation.getPropertyPath().toString(),
                                violation.getMessage()
                        )
                );

        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        ErrorCode.VALIDATION_ERROR.name(),
                        "Existen parámetros inválidos",
                        request.getRequestURI(),
                        MDC.get("correlationId"),
                        validationErrors
                );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {

        ApiErrorResponse response =
                new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR
                                .getReasonPhrase(),
                        ErrorCode.INTERNAL_ERROR.name(),
                        "Ocurrió un error interno",
                        request.getRequestURI(),
                        MDC.get("correlationId"),
                        Map.of()
                );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private record ApiErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String code,
            String message,
            String path,
            String correlationId,
            Map<String, String> validationErrors
    ) {
    }
}