package com.cryptocraft.identityservice.controllers.dto.response;

import com.cryptocraft.security.VenueRole;

public record VenueAccessDto(
        Long tenantId,
        VenueRole role
) {}
