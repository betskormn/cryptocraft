package com.cryptocraft.inventoryservice.exceptions;

public class BrandAlreadyExistsException extends RuntimeException {

    private final String brandName;

    public BrandAlreadyExistsException(String brandName) {
        super("Brand already exists: " + brandName);
        this.brandName = brandName;
    }

    public String getBrandName() {
        return brandName;
    }
}

