package com.cryptocraft.inventoryservice.dao.enums;

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
public enum Unit {

    LITERS(1),
    MILLILITERS(2),
    BOTTLES(3),
    GRAMS(4),
    KILOGRAMS(5),
    PORTION(6),
    PIECE(7),
    BOWL(8),
    GLASS(9),
    SHOT(10);

    private final Integer id;

    private static final Map<Integer, Unit> VALUES = Stream.of(values())
            .collect(collectingAndThen(toMap(Unit::id, Function.identity()), Collections::unmodifiableMap));

    public static Unit byId(final Integer id) {
        return Optional.ofNullable(VALUES.get(id))
                .orElseThrow(() -> new IllegalArgumentException(format("Unit for %d has not presented!", id)));
    }

    public Integer id() {
        return id;
    }

}
