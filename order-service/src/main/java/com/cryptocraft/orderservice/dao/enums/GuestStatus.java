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

/**
 * Whether a waiter has confirmed that the guest really sits at the table (the QR code may have been
 * photographed). Orders of a PENDING guest are held; a REJECTED guest cannot order.
 */
@RequiredArgsConstructor
public enum GuestStatus {

    PENDING(1),
    APPROVED(2),
    REJECTED(3);

    private final int id;

    private static final Map<Integer, GuestStatus> VALUES = Stream.of(values())
            .collect(collectingAndThen(toMap(GuestStatus::id, Function.identity()), Collections::unmodifiableMap));

    public static GuestStatus byId(final Integer id) {
        return Optional.ofNullable(VALUES.get(id))
                .orElseThrow(() -> new IllegalArgumentException(format("Guest status for %d has not presented!", id)));
    }

    public Integer id() {
        return id;
    }
}
