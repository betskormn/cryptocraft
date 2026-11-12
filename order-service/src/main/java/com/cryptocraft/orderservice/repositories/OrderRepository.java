package com.cryptocraft.orderservice.repositories;

import com.cryptocraft.orderservice.dao.Order;
import com.cryptocraft.orderservice.dao.enums.OrderStatus;
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
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("""
           select distinct o
           from Order o
           left join fetch o.lines
           join fetch o.participant
           where o.orderId = :orderId
           """)
    Optional<Order> findByIdWithLines(@Param("orderId") UUID orderId);

    @Query("""
           select distinct o
           from Order o
           left join fetch o.lines
           join fetch o.participant
           where o.clientOrderId = :clientOrderId
           """)
    Optional<Order> findByClientOrderIdWithLines(@Param("clientOrderId") String clientOrderId);

    @Query("""
           select distinct o
           from Order o
           left join fetch o.lines
           join fetch o.participant
           where o.session.sessionId = :sessionId
           order by o.createdAt
           """)
    List<Order> findAllBySessionWithLines(@Param("sessionId") UUID sessionId);

    // serializes changes to one order's lines, so two cancels at once cannot both recompute the total from stale lines
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select distinct o
           from Order o
           left join fetch o.lines
           where o.participant.participantId = :participantId and o.status = :status
           order by o.createdAt
           """)
    List<Order> findAllByParticipantAndStatusWithLines(@Param("participantId") UUID participantId, @Param("status") OrderStatus status);

    @Query("select o from Order o where o.orderId = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") UUID orderId);
}
