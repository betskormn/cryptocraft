package com.cryptocraft.orderservice.controllers.dto.request;

import com.cryptocraft.orderservice.dao.enums.LineStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code reason} is kept only for CANCELLED (e.g. "ran out of lime"); the guest and the waiter see it in the bill.
 */
public record LineStatusRequestDto(
        @NotNull(message = "status must not be null")
        LineStatus status,
        @Size(max = 255, message = "reason must be at most 255 characters")
        String reason
) {}
