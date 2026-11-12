package com.cryptocraft.identityservice.controllers.dto.response;

import java.time.Instant;

public record GuestTokenResponseDto(
        String token,
        Instant expiresAt
) {}
