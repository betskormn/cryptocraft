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
public enum SessionStatus {

    // a visit at a table; closed when the bill is settled
    OPEN(1),
    CLOSED(2);

    private final int id;


    private static final Map<Integer, SessionStatus> VALUES = Stream.of(values())
            .collect(collectingAndThen(toMap(SessionStatus::id, Function.identity()), Collections::unmodifiableMap));

    public static SessionStatus byId(final Integer id) {
        return Optional.ofNullable(VALUES.get(id))
                .orElseThrow(() -> new IllegalArgumentException(format("Session status for %d has not presented!", id)));
    }

    public Integer id() {
        return id;
    }
}
