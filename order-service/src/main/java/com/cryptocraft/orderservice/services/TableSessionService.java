package com.cryptocraft.orderservice.services;

import com.cryptocraft.orderservice.controllers.dto.request.JoinTableRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.JoinTableResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.ParticipantBillResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.SessionBillResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.TableSessionResponseDto;

import java.util.List;
import java.util.UUID;

public interface TableSessionService {

    /**
     * A guest scanned a table's QR code: joins the table's open visit, or opens one.
     */
    JoinTableResponseDto join(JoinTableRequestDto requestDto);

    List<TableSessionResponseDto> getOpenSessions();

    SessionBillResponseDto getSessionBill(UUID sessionId);

    ParticipantBillResponseDto getParticipantBill(UUID participantId);

    TableSessionResponseDto closeSession(UUID sessionId);
}
