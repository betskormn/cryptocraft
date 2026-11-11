package com.cryptocraft.orderservice.controllers.dto.response;

import com.cryptocraft.orderservice.dao.enums.SessionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The bill of a whole table, split per guest (each guest pays for themselves).
 */
public record SessionBillResponseDto(
        UUID sessionId,
        String tableLabel,
        SessionStatus status,
        BigDecimal total,
        List<ParticipantBillResponseDto> participants
) {
    public SessionBillResponseDto {
        participants = (participants == null) ? List.of() : List.copyOf(participants);
    }
}
