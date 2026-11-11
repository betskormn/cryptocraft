package com.cryptocraft.orderservice.exceptions;

/**
 * The request is valid but cannot be accepted in the current state: item on the stop-list, no current
 * price, table session closed, table inactive.
 */
public class OrderRejectedException extends RuntimeException {

    public OrderRejectedException(String message) {
        super(message);
    }
}
