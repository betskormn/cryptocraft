package com.cryptocraft.orderservice.controllers.dto.response;

import com.cryptocraft.orderservice.dao.enums.GuestStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * What one guest has ordered during the visit, each line at the price locked when it was sent.
 * Orders still waiting for a waiter to confirm the guest are listed and counted (AWAITING_APPROVAL).
 */
public record ParticipantBillResponseDto(
        UUID participantId,
        String displayName,
        GuestStatus status,
        BigDecimal total,
        List<OrderResponseDto> orders
) {
    public ParticipantBillResponseDto {
        orders = (orders == null) ? List.of() : List.copyOf(orders);
    }
}
