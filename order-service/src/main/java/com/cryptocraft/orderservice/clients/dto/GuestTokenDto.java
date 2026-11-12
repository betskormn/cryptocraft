package com.cryptocraft.orderservice.clients.dto;

import java.time.Instant;

// Identity POST /api/v1/guest-tokens
public record GuestTokenDto(
        String token,
        Instant expiresAt
) {}
