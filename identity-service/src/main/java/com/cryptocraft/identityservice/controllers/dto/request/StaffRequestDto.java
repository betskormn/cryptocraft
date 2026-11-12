package com.cryptocraft.identityservice.controllers.dto.request;

import com.cryptocraft.security.VenueRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Adds a person to the venue. A new email creates an account with {@code password} (to be changed by its
 * owner); an existing account just gets the venue, and {@code displayName} and {@code password} are ignored.
 */
public record StaffRequestDto(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email is not valid")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @Size(max = 100, message = "Name must be at most 100 characters")
        String displayName,

        @NotNull(message = "Role is required")
        VenueRole role,

        @Size(min = 8, max = 100, message = "Password must be 8 to 100 characters")
        String password
) {}
