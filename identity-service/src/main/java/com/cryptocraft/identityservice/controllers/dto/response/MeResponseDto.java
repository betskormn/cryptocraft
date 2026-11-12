package com.cryptocraft.identityservice.controllers.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * The signed-in person. A platform admin may open any venue, so {@code venues} lists only real memberships.
 */
public record MeResponseDto(
        UUID userId,
        String email,
        String displayName,
        boolean platformAdmin,
        List<VenueAccessDto> venues
) {}
