package com.cryptocraft.inventoryservice.repositories.projections;

import java.math.BigDecimal;

/**
 * One ingredient of an item's recipe, read without loading Product entities.
 */
public record RecipeLine(
        Long itemId,
        Long productId,
        BigDecimal quantity
) {}
