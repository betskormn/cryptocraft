package com.cryptocraft.inventoryservice.repositories;

import com.cryptocraft.inventoryservice.dao.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
}