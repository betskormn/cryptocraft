package com.cryptocraft.inventoryservice.events;

/**
 * Published inside the transaction that created, updated, stopped or deleted an item; turned into a
 * Kafka message only after that transaction commits (see ItemConfigPublisher).
 */
public record ItemChangedEvent(Long itemId) {}
