package com.cryptocraft.orderservice.dao.enums;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public enum StockStatus {

    // recipe write-off not confirmed by Inventory yet (retried by StockWriteOffRetrier)
    PENDING(1),
    WRITTEN_OFF(2),
    // Inventory refused it for good (4xx); not retried, stock has to be corrected by hand
    FAILED(3);

    private final int id;


    private static final Map<Integer, StockStatus> VALUES = Stream.of(values())
            .collect(collectingAndThen(toMap(StockStatus::id, Function.identity()), Collections::unmodifiableMap));

    public static StockStatus byId(final Integer id) {
        return Optional.ofNullable(VALUES.get(id))
                .orElseThrow(() -> new IllegalArgumentException(format("Stock status for %d has not presented!", id)));
    }

    public Integer id() {
        return id;
    }
}
