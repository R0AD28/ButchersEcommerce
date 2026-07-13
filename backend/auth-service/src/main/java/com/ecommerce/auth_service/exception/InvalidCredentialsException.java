package com.ecommerce.auth_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException
        extends BusinessException {

    public InvalidCredentialsException(
            String message
    ) {

        super(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_CREDENTIALS,
                message
        );
    }
}