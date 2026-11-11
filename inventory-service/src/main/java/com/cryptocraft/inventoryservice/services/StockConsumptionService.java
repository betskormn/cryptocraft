package com.cryptocraft.inventoryservice.services;

import com.cryptocraft.inventoryservice.controllers.dto.request.StockConsumptionRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.StockConsumptionResponseDto;

public interface StockConsumptionService {

    /**
     * Writes off the recipes of an order's items. A repeated call with the same orderId changes
     * nothing and returns the first result with {@code created = false}.
     */
    Result consume(StockConsumptionRequestDto requestDto);

    StockConsumptionResponseDto getByOrderId(String orderId);

    record Result(StockConsumptionResponseDto consumption, boolean created) {}
}
