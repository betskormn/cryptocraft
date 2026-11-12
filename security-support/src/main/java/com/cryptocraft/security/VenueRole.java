package com.cryptocraft.security;

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
 * What a staff member may do in one venue. A person can work in several venues with a different role in each.
 */
@RequiredArgsConstructor
public enum VenueRole {

    // owner / manager: everything in the venue, including staff and tables
    ADMIN(1),
    // menu, recipes and stock
    INVENTORY_MANAGER(2),
    // floor, kitchen and bar: visits, bills, kitchen/bar screens, stop-list
    STAFF(3);

    private final int id;

    private static final Map<Integer, VenueRole> VALUES = Stream.of(values())
            .collect(collectingAndThen(toMap(VenueRole::id, Function.identity()), Collections::unmodifiableMap));

    public static VenueRole byId(final Integer id) {
        return Optional.ofNullable(VALUES.get(id))
                .orElseThrow(() -> new IllegalArgumentException(format("Venue role for %d has not presented!", id)));
    }

    public Integer id() {
        return id;
    }
}
