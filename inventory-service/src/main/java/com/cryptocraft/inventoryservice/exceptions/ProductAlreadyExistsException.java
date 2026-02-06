package com.cryptocraft.inventoryservice.exceptions;

public class ProductAlreadyExistsException extends RuntimeException {
    public ProductAlreadyExistsException(String name, Long brandId) {
        super("Product already exists: name=" + name + ", brandId=" + brandId);
    }
}

