package com.cryptocraft.inventoryservice.repositories;

import com.cryptocraft.inventoryservice.dao.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {


    @Query("""
           select distinct i
           from Item i
           left join fetch i.productLinks l
           left join fetch l.product p
           where i.itemId = :id
           """)
    Optional<Item> findByIdWithProducts(Long id);

    @Query("""
           select distinct i
           from Item i
           left join fetch i.productLinks l
           left join fetch l.product p
           """)
    List<Item> findAllWithProducts();

}
