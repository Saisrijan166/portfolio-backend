package com.srijan.portfolio.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }
}
