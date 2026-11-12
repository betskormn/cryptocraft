package com.cryptocraft.orderservice.config;

import com.cryptocraft.orderservice.exceptions.DownstreamUnavailableException;
import com.cryptocraft.orderservice.exceptions.OrderRejectedException;
import com.cryptocraft.support.web.BaseApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Order Service's own exceptions on top of the shared ProblemDetail mapping (service-support).
 */
@RestControllerAdvice
public class CryptoCraftApiControllerAdvice extends BaseApiExceptionHandler {

    @ExceptionHandler(OrderRejectedException.class)
    public ProblemDetail handleRejected(OrderRejectedException ex) {
        return problem(HttpStatus.CONFLICT, "Rejected", ex.getMessage());
    }

    @ExceptionHandler(DownstreamUnavailableException.class)
    public ProblemDetail handleDownstream(DownstreamUnavailableException ex) {
        log.warn("{}: {}", ex.getMessage(), ex.getCause() != null ? ex.getCause().getMessage() : "");
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable", ex.getMessage());
    }
}
