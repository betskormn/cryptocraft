package com.cryptocraft.orderservice.repositories;

import com.cryptocraft.orderservice.dao.SessionParticipant;
import com.cryptocraft.orderservice.dao.enums.GuestStatus;
import com.cryptocraft.orderservice.dao.enums.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, UUID> {

    @Query("""
           select p
           from SessionParticipant p
           join fetch p.session s
           join fetch s.table
           where p.participantId = :participantId
           """)
    Optional<SessionParticipant> findByIdWithSession(@Param("participantId") UUID participantId);

    List<SessionParticipant> findAllBySession_SessionIdOrderByCreatedAt(UUID sessionId);

    // a guest's order and a waiter's decision about the guest never pass each other
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select p
           from SessionParticipant p
           join fetch p.session s
           join fetch s.table
           where p.participantId = :participantId
           """)
    Optional<SessionParticipant> findByIdForUpdate(@Param("participantId") UUID participantId);

    // guests waiting for a waiter, per open visit
    @Query("""
           select p.session.sessionId as sessionId, count(p) as guests
           from SessionParticipant p
           where p.status = :status and p.session.status = :sessionStatus
           group by p.session.sessionId
           """)
    List<GuestCount> countPerSession(@Param("status") GuestStatus status, @Param("sessionStatus") SessionStatus sessionStatus);

    interface GuestCount {
        UUID getSessionId();

        long getGuests();
    }
}
