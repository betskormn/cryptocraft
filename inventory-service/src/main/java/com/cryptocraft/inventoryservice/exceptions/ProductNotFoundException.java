package com.cryptocraft.inventoryservice.exceptions;

import java.util.List;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(List<Long> missingIds) {
        super("Products not found: " + missingIds);
    }
}

