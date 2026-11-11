package com.cryptocraft.orderservice.services.impl;

import com.cryptocraft.orderservice.controllers.dto.request.JoinTableRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.JoinTableResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.OrderResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.ParticipantBillResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.SessionBillResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.TableSessionResponseDto;
import com.cryptocraft.orderservice.dao.Order;
import com.cryptocraft.orderservice.dao.SessionParticipant;
import com.cryptocraft.orderservice.dao.TableSession;
import com.cryptocraft.orderservice.dao.VenueTable;
import com.cryptocraft.orderservice.dao.enums.OrderStatus;
import com.cryptocraft.orderservice.dao.enums.SessionStatus;
import com.cryptocraft.orderservice.exceptions.OrderRejectedException;
import com.cryptocraft.orderservice.mappers.OrderMapper;
import com.cryptocraft.orderservice.mappers.TableSessionMapper;
import com.cryptocraft.orderservice.repositories.OrderRepository;
import com.cryptocraft.orderservice.repositories.SessionParticipantRepository;
import com.cryptocraft.orderservice.repositories.TableSessionRepository;
import com.cryptocraft.orderservice.repositories.VenueTableRepository;
import com.cryptocraft.orderservice.services.TableSessionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableSessionServiceImpl implements TableSessionService {

    private final VenueTableRepository venueTableRepository;
    private final TableSessionRepository tableSessionRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final OrderRepository orderRepository;
    private final TableSessionMapper tableSessionMapper;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public JoinTableResponseDto join(JoinTableRequestDto dto) {
        VenueTable table = venueTableRepository.findByCode(dto.tableCode().trim().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Table not found"));
        if (!table.isActive()) {
            throw new OrderRejectedException(format("Table '%s' is not in service", table.getLabel()));
        }

        TableSession session = tableSessionRepository.findByTable_TableIdAndStatus(table.getTableId(), SessionStatus.OPEN)
                .orElseGet(() -> {
                    TableSession opened = tableSessionRepository.save(TableSession.builder()
                            .table(table)
                            .status(SessionStatus.OPEN)
                            .build());
                    log.info("Session:{} opened at table:{}", opened.getSessionId(), table.getLabel());
                    return opened;
                });

        String displayName = dto.displayName() == null || dto.displayName().isBlank() ? null : dto.displayName().trim();
        SessionParticipant participant = sessionParticipantRepository.save(SessionParticipant.builder()
                .session(session)
                .displayName(displayName)
                .build());
        log.info("Participant:{} joined session:{}", participant.getParticipantId(), session.getSessionId());
        return new JoinTableResponseDto(session.getSessionId(), participant.getParticipantId(), table.getTableId(), table.getLabel());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableSessionResponseDto> getOpenSessions() {
        return tableSessionRepository.findAllByStatusWithTable(SessionStatus.OPEN).stream()
                .map(tableSessionMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SessionBillResponseDto getSessionBill(UUID sessionId) {
        TableSession session = findSession(sessionId);
        Map<UUID, List<Order>> ordersByParticipant = orderRepository.findAllBySessionWithLines(sessionId).stream()
                .collect(Collectors.groupingBy(order -> order.getParticipant().getParticipantId()));

        List<ParticipantBillResponseDto> participants = sessionParticipantRepository
                .findAllBySession_SessionIdOrderByCreatedAt(sessionId).stream()
                .map(p -> bill(p, ordersByParticipant.getOrDefault(p.getParticipantId(), List.of())))
                .toList();
        BigDecimal total = participants.stream()
                .map(ParticipantBillResponseDto::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SessionBillResponseDto(sessionId, session.getTable().getLabel(), session.getStatus(), total, participants);
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantBillResponseDto getParticipantBill(UUID participantId) {
        SessionParticipant participant = sessionParticipantRepository.findById(participantId)
                .orElseThrow(() -> new EntityNotFoundException("Participant not found"));
        List<Order> orders = orderRepository.findAllBySessionWithLines(participant.getSession().getSessionId()).stream()
                .filter(order -> order.getParticipant().getParticipantId().equals(participantId))
                .toList();
        return bill(participant, orders);
    }

    // closing is the staff's call (bill settled); afterwards no order can be added to the visit
    @Override
    @Transactional
    public TableSessionResponseDto closeSession(UUID sessionId) {
        TableSession session = findSession(sessionId);
        if (session.getStatus() == SessionStatus.CLOSED) {
            throw new OrderRejectedException("Session is already closed");
        }
        session.setStatus(SessionStatus.CLOSED);
        session.setClosedAt(Instant.now());
        log.info("Session:{} at table:{} closed", sessionId, session.getTable().getLabel());
        return tableSessionMapper.toDto(tableSessionRepository.save(session));
    }

    private TableSession findSession(UUID sessionId) {
        return tableSessionRepository.findByIdWithTable(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(format("Session with id:%s, not founded", sessionId)));
    }

    // cancelled orders are listed but not charged
    private ParticipantBillResponseDto bill(SessionParticipant participant, List<Order> orders) {
        List<OrderResponseDto> orderDtos = orders.stream().map(orderMapper::toDto).toList();
        BigDecimal total = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PLACED)
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ParticipantBillResponseDto(participant.getParticipantId(), participant.getDisplayName(), total, orderDtos);
    }
}
