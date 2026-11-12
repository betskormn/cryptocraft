package com.cryptocraft.orderservice.repositories;

import com.cryptocraft.orderservice.dao.OrderLine;
import com.cryptocraft.orderservice.dao.enums.ItemType;
import com.cryptocraft.orderservice.dao.enums.LineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    // the queue of one station in the order the guests sent it
    @Query("""
           select l
           from OrderLine l
           join fetch l.order o
           join fetch o.session s
           join fetch s.table
           join fetch o.participant
           where l.itemType = :itemType
             and l.status in :statuses
             and o.sentAt is not null
           order by o.sentAt, l.lineId
           """)
    List<OrderLine> findQueue(@Param("itemType") ItemType itemType, @Param("statuses") Collection<LineStatus> statuses);

    @Query("select l.order.orderId from OrderLine l where l.lineId = :lineId")
    Optional<UUID> findOrderIdByLineId(@Param("lineId") Long lineId);

    @Query("""
           select l
           from OrderLine l
           join fetch l.order o
           join fetch o.session s
           join fetch s.table
           join fetch o.participant
           where l.lineId = :lineId
           """)
    Optional<OrderLine> findByIdForKitchen(@Param("lineId") Long lineId);
}
