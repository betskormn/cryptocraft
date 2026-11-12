package com.cryptocraft.orderservice.controllers.dto.response;

import com.cryptocraft.orderservice.dao.enums.GuestStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code guestToken} is the guest's bearer token for this visit (ordering, their bill). A new guest is
 * PENDING: their orders wait until a waiter confirms them. Venue name, slug (for the live prices socket)
 * and currency are for the guest's page.
 */
public record JoinTableResponseDto(
        UUID sessionId,
        UUID participantId,
        Long tableId,
        String tableLabel,
        GuestStatus status,
        String guestToken,
        Instant tokenExpiresAt,
        String venueName,
        String venueSlug,
        String currency
) {}
