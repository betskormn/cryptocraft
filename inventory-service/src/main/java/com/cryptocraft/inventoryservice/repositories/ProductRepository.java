package com.cryptocraft.inventoryservice.repositories;

import com.cryptocraft.inventoryservice.dao.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByNameIgnoreCaseAndBrand_BrandId(String name, Long brandId);

    @Modifying
    @Transactional
    @Query("""
        update Product p 
        set p.quantityInStock = p.quantityInStock + :delta
        where p.id = :id
    """)
    int incrementStock(@Param("id") Long id, @Param("delta") Integer delta);

}