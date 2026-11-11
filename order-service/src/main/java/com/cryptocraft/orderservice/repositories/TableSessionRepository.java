package com.cryptocraft.orderservice.repositories;

import com.cryptocraft.orderservice.dao.TableSession;
import com.cryptocraft.orderservice.dao.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableSessionRepository extends JpaRepository<TableSession, UUID> {

    Optional<TableSession> findByTable_TableIdAndStatus(Long tableId, SessionStatus status);

    @Query("""
           select s
           from TableSession s
           join fetch s.table
           where s.status = :status
           order by s.createdAt
           """)
    List<TableSession> findAllByStatusWithTable(@Param("status") SessionStatus status);

    @Query("""
           select s
           from TableSession s
           join fetch s.table
           where s.sessionId = :sessionId
           """)
    Optional<TableSession> findByIdWithTable(@Param("sessionId") UUID sessionId);
}
