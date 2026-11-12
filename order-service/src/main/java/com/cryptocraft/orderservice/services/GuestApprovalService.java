package com.cryptocraft.orderservice.services;

import com.cryptocraft.orderservice.controllers.dto.response.ParticipantBillResponseDto;

import java.util.UUID;

public interface GuestApprovalService {

    /**
     * The waiter saw the guest at the table: their held orders go to the kitchen/bar.
     */
    ParticipantBillResponseDto approve(UUID sessionId, UUID participantId);

    /**
     * Nobody of that name at the table: their held orders are cancelled and they cannot order.
     */
    ParticipantBillResponseDto reject(UUID sessionId, UUID participantId);
}
