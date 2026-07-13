package com.ecommerce.commerce_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode errorCode;

    public BusinessException(
            HttpStatus status,
            ErrorCode errorCode,
            String message
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
