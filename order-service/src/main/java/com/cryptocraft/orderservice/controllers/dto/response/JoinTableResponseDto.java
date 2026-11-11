package com.cryptocraft.orderservice.controllers.dto.response;

import java.util.UUID;

/**
 * {@code participantId} is the guest's token for this visit: send it as X-Participant-Id when ordering.
 */
public record JoinTableResponseDto(
        UUID sessionId,
        UUID participantId,
        Long tableId,
        String tableLabel
) {}
