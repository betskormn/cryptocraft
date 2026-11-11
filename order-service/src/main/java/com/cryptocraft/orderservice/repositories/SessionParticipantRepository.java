package com.cryptocraft.orderservice.repositories;

import com.cryptocraft.orderservice.dao.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
