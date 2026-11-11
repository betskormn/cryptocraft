package com.cryptocraft.orderservice.exceptions;

/**
 * Inventory or Pricing could not answer, so an order cannot be priced or checked right now.
 */
public class DownstreamUnavailableException extends RuntimeException {

    public DownstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
