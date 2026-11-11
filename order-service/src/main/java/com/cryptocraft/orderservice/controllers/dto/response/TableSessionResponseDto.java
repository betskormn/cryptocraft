package com.cryptocraft.orderservice.controllers.dto.response;

import com.cryptocraft.orderservice.dao.enums.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public record TableSessionResponseDto(
        UUID sessionId,
        Long tableId,
        String tableLabel,
        SessionStatus status,
        Instant openedAt,
        Instant closedAt
) {}
