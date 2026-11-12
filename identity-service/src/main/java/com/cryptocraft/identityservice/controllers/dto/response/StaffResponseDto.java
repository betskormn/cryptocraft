package com.cryptocraft.identityservice.controllers.dto.response;

import com.cryptocraft.security.VenueRole;

import java.util.UUID;

public record StaffResponseDto(
        UUID userId,
        String email,
        String displayName,
        VenueRole role,
        boolean enabled
) {}
