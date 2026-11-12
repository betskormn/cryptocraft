package com.cryptocraft.identityservice.controllers.dto.request;

import com.cryptocraft.security.VenueRole;
import jakarta.validation.constraints.NotNull;

public record StaffRoleRequestDto(
        @NotNull(message = "Role is required")
        VenueRole role
) {}
