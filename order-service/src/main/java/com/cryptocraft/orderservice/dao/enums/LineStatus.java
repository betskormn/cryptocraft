package com.cryptocraft.orderservice.dao.enums;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

/**
 * Where a line is on the kitchen/bar screen. NEW, PREPARING and READY are still in work and may be
 * changed in any direction (a mis-tap is fixed by tapping again); SERVED and CANCELLED are final.
 */
@RequiredArgsConstructor
public enum LineStatus {

    // sent by the guest, nobody has started it
    NEW(1),
    PREPARING(2),
    // waiting at the pass for the waiter
    READY(3),
    SERVED(4),
    // not made (e.g. ran out after the order was sent); not charged
    CANCELLED(5);

    private final int id;

    public static final Set<LineStatus> IN_WORK = Collections.unmodifiableSet(EnumSet.of(NEW, PREPARING, READY));

    private static final Map<Integer, LineStatus> VALUES = Stream.of(values())
            .collect(collectingAndThen(toMap(LineStatus::id, Function.identity()), Collections::unmodifiableMap));

    public static LineStatus byId(final Integer id) {
        return Optional.ofNullable(VALUES.get(id))
                .orElseThrow(() -> new IllegalArgumentException(format("Line status for %d has not presented!", id)));
    }

    public Integer id() {
        return id;
    }

    public boolean isFinal() {
        return !IN_WORK.contains(this);
    }
}
