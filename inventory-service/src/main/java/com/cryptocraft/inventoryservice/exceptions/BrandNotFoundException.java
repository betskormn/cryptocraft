package com.cryptocraft.inventoryservice.exceptions;

import java.util.List;

public class BrandNotFoundException extends RuntimeException {
    public BrandNotFoundException(Long brandId) {
        super("Brand not found: " + brandId);
    }

    public BrandNotFoundException(List<Long> brandIds) {
        super("Brands not found: " + brandIds);
    }
}
