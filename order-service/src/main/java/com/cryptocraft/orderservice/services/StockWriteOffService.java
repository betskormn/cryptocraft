package com.cryptocraft.orderservice.services;

import java.util.UUID;

public interface StockWriteOffService {

    /**
     * Asks Inventory to write off the recipes of a placed order (idempotent by order id there) and marks
     * the order WRITTEN_OFF. If Inventory is unreachable the order stays PENDING for StockWriteOffRetrier;
     * if Inventory refuses it (4xx) the order becomes FAILED and is not retried.
     *
     * @return true when the write-off is confirmed
     */
    boolean writeOff(UUID orderId);
}
