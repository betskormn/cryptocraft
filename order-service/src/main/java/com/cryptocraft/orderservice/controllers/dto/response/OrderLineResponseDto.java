package com.cryptocraft.orderservice.controllers.dto.response;

import com.cryptocraft.orderservice.dao.enums.ItemType;

import java.math.BigDecimal;

public record OrderLineResponseDto(
        Long itemId,
        String itemName,
        ItemType itemType,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
