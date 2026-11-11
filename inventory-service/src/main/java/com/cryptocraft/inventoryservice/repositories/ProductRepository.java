package com.cryptocraft.inventoryservice.repositories;

import com.cryptocraft.inventoryservice.dao.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByNameIgnoreCaseAndBrand_BrandId(String name, Long brandId);

    boolean existsByNameIgnoreCaseAndBrand_BrandIdAndProductIdNot(String name, Long brandId, Long productId);

    long countByBrand_BrandId(Long brandId);

    // locks the rows for a recipe write-off; ordered by id so concurrent orders lock in the same order
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId in :ids order by p.productId")
    List<Product> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    @Query("""
        select l.item.name
        from ItemProduct l
        where l.product.productId = :productId
        order by l.item.name
    """)
    List<String> findItemNamesUsingProduct(@Param("productId") Long productId);

    /**
     * Adds delta (negative for a write-off) unless the stock would go below zero.
     * Returns 0 when the product does not exist or the stock is insufficient.
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
        update Product p
        set p.quantityInStock = p.quantityInStock + :delta
        where p.id = :id and p.quantityInStock + :delta >= 0
    """)
    int incrementStock(@Param("id") Long id, @Param("delta") BigDecimal delta);
}
