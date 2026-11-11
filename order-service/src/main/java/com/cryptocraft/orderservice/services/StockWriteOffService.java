package com.cryptocraft.orderservice.services;

import java.util.UUID;

public interface StockWriteOffService {

    /**
     * Asks Inventory to write off the recipes of a placed order (idempotent by order id there) and marks
     * the order WRITTEN_OFF. On failure the order stays PENDING for StockWriteOffRetrier.
     *
     * @return true when the write-off is confirmed
     */
    boolean writeOff(UUID orderId);
}
