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
public enum TenantStatus {

    ACTIVE(1),
    SUSPENDED(2),
    // the venue new demo venues copy their menu from; editable, but gets no live prices
    TEMPLATE(3);

    private final int id;


    private static final Map<Integer, TenantStatus> VALUES = Stream.of(values())
            .collect(collectingAndThen(toMap(TenantStatus::id, Function.identity()), Collections::unmodifiableMap));

    public static TenantStatus byId(final Integer id) {
        return Optional.ofNullable(VALUES.get(id))
                .orElseThrow(() -> new IllegalArgumentException(format("Tenant status for %d has not presented!", id)));
    }

    public Integer id() {
        return id;
    }
}
