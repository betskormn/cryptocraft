package com.cryptocraft.inventoryservice.config;

import com.cryptocraft.inventoryservice.exceptions.BrandAlreadyExistsException;
import com.cryptocraft.inventoryservice.exceptions.BrandNotFoundException;
import com.cryptocraft.inventoryservice.exceptions.EntityInUseException;
import com.cryptocraft.inventoryservice.exceptions.InsufficientStockException;
import com.cryptocraft.inventoryservice.exceptions.ProductAlreadyExistsException;
import com.cryptocraft.inventoryservice.exceptions.ProductNotFoundException;
import com.cryptocraft.inventoryservice.exceptions.TenantAlreadyExistsException;
import com.cryptocraft.support.web.BaseApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Inventory's own exceptions on top of the shared ProblemDetail mapping (service-support).
 */
@RestControllerAdvice
public class CryptoCraftApiControllerAdvice extends BaseApiExceptionHandler {

    @ExceptionHandler({BrandNotFoundException.class, ProductNotFoundException.class})
    public ProblemDetail handleCatalogNotFound(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    @ExceptionHandler({BrandAlreadyExistsException.class, ProductAlreadyExistsException.class,
            TenantAlreadyExistsException.class})
    public ProblemDetail handleAlreadyExists(RuntimeException ex) {
        return problem(HttpStatus.CONFLICT, "Already exists", ex.getMessage());
    }

    @ExceptionHandler({EntityInUseException.class, InsufficientStockException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }
}
