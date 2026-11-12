package com.cryptocraft.orderservice.controllers.dto.response;

import com.cryptocraft.orderservice.dao.enums.ItemType;
import com.cryptocraft.orderservice.dao.enums.LineStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * One line on the kitchen/bar screen: what to make, for which table and guest, and since when.
 */
public record KitchenLineResponseDto(
        Long lineId,
        UUID orderId,
        String tableLabel,
        String guestName,
        Long itemId,
        String itemName,
        ItemType itemType,
        Integer quantity,
        LineStatus status,
        String cancelReason,
        Instant placedAt,
        Instant statusChangedAt
) {}
