package com.cryptocraft.identityservice.config;

import com.cryptocraft.identityservice.exceptions.StaffConflictException;
import com.cryptocraft.support.web.BaseApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Identity Service's own exceptions on top of the shared ProblemDetail mapping (service-support).
 */
@RestControllerAdvice
public class CryptoCraftApiControllerAdvice extends BaseApiExceptionHandler {

    @ExceptionHandler(StaffConflictException.class)
    public ProblemDetail handleConflict(StaffConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }
}
