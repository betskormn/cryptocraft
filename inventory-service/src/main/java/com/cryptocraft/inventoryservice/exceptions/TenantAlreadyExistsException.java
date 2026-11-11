package com.cryptocraft.inventoryservice.exceptions;

public class TenantAlreadyExistsException extends RuntimeException {

    public TenantAlreadyExistsException(String message) {
        super(message);
    }
}
