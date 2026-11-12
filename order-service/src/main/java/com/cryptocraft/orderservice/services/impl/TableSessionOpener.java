package com.cryptocraft.orderservice.services.impl;

import com.cryptocraft.orderservice.dao.TableSession;
import com.cryptocraft.orderservice.dao.VenueTable;
import com.cryptocraft.orderservice.dao.enums.SessionStatus;
import com.cryptocraft.orderservice.repositories.TableSessionRepository;
import com.cryptocraft.orderservice.repositories.VenueTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Opens a visit in its own transaction, so a lost race on the one-open-visit-per-table index fails
 * only this insert and the caller can pick up the visit the other guest just opened.
 */
@Component
@RequiredArgsConstructor
class TableSessionOpener {

    private final VenueTableRepository venueTableRepository;
    private final TableSessionRepository tableSessionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID open(Long tableId) {
        VenueTable table = venueTableRepository.getReferenceById(tableId);
        return tableSessionRepository.saveAndFlush(TableSession.builder()
                        .table(table)
                        .status(SessionStatus.OPEN)
                        .build())
                .getSessionId();
    }
}
