package com.cryptocraft.orderservice.services;

import com.cryptocraft.orderservice.controllers.dto.request.JoinTableRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.JoinTableResponseDto;

import java.util.UUID;

public interface GuestJoinService {

    /**
     * Joins the table's visit and gives the guest their token (signed by identity-service).
     */
    JoinTableResponseDto join(JoinTableRequestDto requestDto, UUID currentParticipantId);
}
