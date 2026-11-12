package com.cryptocraft.orderservice.exceptions;

/**
 * Inventory refused the write-off for good (e.g. an item of the order was deleted meanwhile);
 * retrying cannot help, unlike {@link DownstreamUnavailableException}.
 */
public class StockWriteOffRejectedException extends RuntimeException {

    public StockWriteOffRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
