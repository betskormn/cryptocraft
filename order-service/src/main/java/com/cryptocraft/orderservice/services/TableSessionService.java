package com.cryptocraft.orderservice.services;

import com.cryptocraft.orderservice.controllers.dto.request.JoinTableRequestDto;
import com.cryptocraft.orderservice.dao.enums.GuestStatus;
import com.cryptocraft.orderservice.controllers.dto.response.ParticipantBillResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.SessionBillResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.TableSessionResponseDto;

import java.util.List;
import java.util.UUID;

public interface TableSessionService {

    /**
     * A guest scanned a table's QR code: joins the table's open visit, or opens one. A guest who is already
     * in this visit ({@code currentParticipantId} from their token) stays the same guest.
     */
    JoinedGuest join(JoinTableRequestDto requestDto, UUID currentParticipantId);

    List<TableSessionResponseDto> getOpenSessions();

    SessionBillResponseDto getSessionBill(UUID sessionId);

    ParticipantBillResponseDto getParticipantBill(UUID participantId);

    TableSessionResponseDto closeSession(UUID sessionId);

    record JoinedGuest(UUID sessionId, UUID participantId, Long tableId, String tableLabel, GuestStatus status) {}
}
