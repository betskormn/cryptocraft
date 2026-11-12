package com.cryptocraft.orderservice.services.impl;

import com.cryptocraft.orderservice.controllers.dto.response.ParticipantBillResponseDto;
import com.cryptocraft.orderservice.dao.Order;
import com.cryptocraft.orderservice.dao.OrderLine;
import com.cryptocraft.orderservice.dao.SessionParticipant;
import com.cryptocraft.orderservice.dao.enums.GuestStatus;
import com.cryptocraft.orderservice.dao.enums.LineStatus;
import com.cryptocraft.orderservice.dao.enums.OrderStatus;
import com.cryptocraft.orderservice.events.OrderPlacedEvent;
import com.cryptocraft.orderservice.exceptions.OrderRejectedException;
import com.cryptocraft.orderservice.repositories.OrderRepository;
import com.cryptocraft.orderservice.repositories.SessionParticipantRepository;
import com.cryptocraft.orderservice.services.GuestApprovalService;
import com.cryptocraft.orderservice.services.TableSessionService;
import com.cryptocraft.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A waiter confirms that a new guest really sits at the table (anyone with a photo of the QR code could join).
 * The guest row is locked, the same lock an order of that guest takes, so no order slips past the decision.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestApprovalServiceImpl implements GuestApprovalService {

    static final String NOT_CONFIRMED = "Guest not confirmed by the staff";

    private final SessionParticipantRepository sessionParticipantRepository;
    private final OrderRepository orderRepository;
    private final TableSessionService tableSessionService;
    // stock write-off and Kafka OrderPlaced happen after commit (OrderPlacedListener)
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ParticipantBillResponseDto approve(UUID sessionId, UUID participantId) {
        SessionParticipant guest = findGuest(sessionId, participantId);
        if (guest.getStatus() == GuestStatus.REJECTED) {
            throw new OrderRejectedException("This guest was already rejected");
        }
        if (guest.getStatus() == GuestStatus.PENDING) {
            Instant now = Instant.now();
            guest.setStatus(GuestStatus.APPROVED);
            guest.setDecidedAt(now);
            List<Order> held = orderRepository.findAllByParticipantAndStatusWithLines(participantId, OrderStatus.AWAITING_APPROVAL);
            held.forEach(order -> {
                order.setStatus(OrderStatus.PLACED);
                order.setSentAt(now);
                eventPublisher.publishEvent(new OrderPlacedEvent(order.getOrderId(), TenantContext.getRequired()));
            });
            log.info("Participant:{} confirmed at session:{}, {} held order(s) sent", participantId, sessionId, held.size());
        }
        return tableSessionService.getParticipantBill(participantId);
    }

    @Override
    @Transactional
    public ParticipantBillResponseDto reject(UUID sessionId, UUID participantId) {
        SessionParticipant guest = findGuest(sessionId, participantId);
        if (guest.getStatus() == GuestStatus.APPROVED) {
            throw new OrderRejectedException("This guest is already confirmed; cancel single lines instead");
        }
        if (guest.getStatus() == GuestStatus.PENDING) {
            Instant now = Instant.now();
            guest.setStatus(GuestStatus.REJECTED);
            guest.setDecidedAt(now);
            List<Order> held = orderRepository.findAllByParticipantAndStatusWithLines(participantId, OrderStatus.AWAITING_APPROVAL);
            held.forEach(order -> cancel(order, now));
            log.info("Participant:{} rejected at session:{}, {} held order(s) cancelled", participantId, sessionId, held.size());
        }
        return tableSessionService.getParticipantBill(participantId);
    }

    // never reached the kitchen or the stock: nothing else to undo
    private static void cancel(Order order, Instant now) {
        for (OrderLine line : order.getLines()) {
            line.setStatus(LineStatus.CANCELLED);
            line.setStatusChangedAt(now);
            line.setCancelReason(NOT_CONFIRMED);
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setTotal(BigDecimal.ZERO);
    }

    private SessionParticipant findGuest(UUID sessionId, UUID participantId) {
        return sessionParticipantRepository.findByIdForUpdate(participantId)
                .filter(p -> p.getSession().getSessionId().equals(sessionId))
                .orElseThrow(() -> new EntityNotFoundException("Guest not found at this visit"));
    }
}
