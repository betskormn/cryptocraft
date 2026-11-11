package com.cryptocraft.inventoryservice.exceptions;

/**
 * Deleting the entity would silently break data that references it (recipes, products of a brand).
 */
public class EntityInUseException extends RuntimeException {

    public EntityInUseException(String message) {
        super(message);
    }
}
