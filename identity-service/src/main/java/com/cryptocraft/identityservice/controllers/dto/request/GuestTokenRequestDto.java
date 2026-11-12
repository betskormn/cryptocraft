package com.cryptocraft.identityservice.controllers.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * A guest who joined a table: Order Service asks for their token.
 */
public record GuestTokenRequestDto(
        @NotNull(message = "tenantId is required")
        @Positive(message = "tenantId must be positive")
        Long tenantId,

        @NotNull(message = "participantId is required")
        UUID participantId,

        @NotNull(message = "sessionId is required")
        UUID sessionId
) {}
