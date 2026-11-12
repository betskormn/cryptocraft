package com.cryptocraft.orderservice.controllers.dto.response;

import com.cryptocraft.orderservice.dao.enums.ItemType;
import com.cryptocraft.orderservice.dao.enums.LineStatus;

import java.math.BigDecimal;

/**
 * A CANCELLED line stays in the bill for reference but is not charged.
 */
public record OrderLineResponseDto(
        Long lineId,
        Long itemId,
        String itemName,
        ItemType itemType,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        LineStatus status,
        String cancelReason
) {}
