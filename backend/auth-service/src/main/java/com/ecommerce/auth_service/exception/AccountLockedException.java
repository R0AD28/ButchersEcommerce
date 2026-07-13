package com.ecommerce.auth_service.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException
        extends BusinessException {

    public AccountLockedException(
            String message
    ) {

        super(
                HttpStatus.LOCKED,
                ErrorCode.ACCOUNT_LOCKED,
                message
        );
    }
}