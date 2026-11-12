package com.cryptocraft.orderservice.clients.dto;

import java.util.UUID;

// Identity POST /api/v1/guest-tokens
public record GuestTokenRequestDto(
        Long tenantId,
        UUID participantId,
        UUID sessionId
) {}
