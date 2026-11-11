package com.cryptocraft.orderservice.repositories;

import com.cryptocraft.orderservice.dao.VenueTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VenueTableRepository extends JpaRepository<VenueTable, Long> {

    Optional<VenueTable> findByCode(String code);

    boolean existsByLabelIgnoreCase(String label);

    boolean existsByLabelIgnoreCaseAndTableIdNot(String label, Long tableId);

    List<VenueTable> findAllByOrderByLabelAsc();
}
