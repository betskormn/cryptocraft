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
public enum ItemType {

    // same ids as Inventory's Type
    MEAL(1),
    DRINK(2);

    private final int id;


    private static final Map<Integer, ItemType> VALUES = Stream.of(values())
            .collect(collectingAndThen(toMap(ItemType::id, Function.identity()), Collections::unmodifiableMap));

    public static ItemType byId(final Integer id) {
        return Optional.ofNullable(VALUES.get(id))
                .orElseThrow(() -> new IllegalArgumentException(format("Item type for %d has not presented!", id)));
    }

    public Integer id() {
        return id;
    }
}
