package com.cryptocraft.orderservice.repositories;

import com.cryptocraft.orderservice.dao.Order;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
