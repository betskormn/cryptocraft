package com.cryptocraft.inventoryservice.repositories;

import com.cryptocraft.inventoryservice.dao.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    boolean existsBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    Optional<Tenant> findBySlug(String slug);
}
