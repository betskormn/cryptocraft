package com.cryptocraft.inventoryservice.exceptions;

import java.math.BigDecimal;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, BigDecimal delta) {
        super("Stock of product " + productId + " cannot go below zero (change: " + delta.stripTrailingZeros().toPlainString() + ")");
    }
}
