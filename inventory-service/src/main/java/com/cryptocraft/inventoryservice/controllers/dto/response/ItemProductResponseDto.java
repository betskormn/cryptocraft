package com.cryptocraft.inventoryservice.controllers.dto.response;

import java.math.BigDecimal;

public record ItemProductResponseDto(
        Long productId,
        String name,
        BigDecimal quantity
) {}
