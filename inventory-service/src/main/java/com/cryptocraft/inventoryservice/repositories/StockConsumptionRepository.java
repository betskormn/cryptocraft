package com.cryptocraft.inventoryservice.repositories;

import com.cryptocraft.inventoryservice.dao.StockConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockConsumptionRepository extends JpaRepository<StockConsumption, Long> {

    @Query("""
           select distinct c
           from StockConsumption c
           left join fetch c.lines
           where c.orderId = :orderId
           """)
    Optional<StockConsumption> findByOrderIdWithLines(@Param("orderId") String orderId);
}
