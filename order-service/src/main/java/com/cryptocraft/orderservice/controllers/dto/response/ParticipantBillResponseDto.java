package com.cryptocraft.orderservice.controllers.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * What one guest has ordered during the visit, each line at the price locked when it was sent.
 */
public record ParticipantBillResponseDto(
        UUID participantId,
        String displayName,
        BigDecimal total,
        List<OrderResponseDto> orders
) {
    public ParticipantBillResponseDto {
        orders = (orders == null) ? List.of() : List.copyOf(orders);
    }
}
