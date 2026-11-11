package com.cryptocraft.orderservice.controllers.dto.response;

public record VenueTableResponseDto(
        Long tableId,
        String label,
        String code,
        boolean active
) {}
