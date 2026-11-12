package com.cryptocraft.pricingservice.pricing.cache;

import com.cryptocraft.pricingservice.dto.ItemPriceDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriceConfigCacheTest {

    private final PriceConfigCache cache = new PriceConfigCache();

    @Test
    void refreshFetchedBeforeAnEventDoesNotUndoIt() {
        long fetchStartedAt = System.currentTimeMillis() - 1000;
        cache.upsert(item(1L, true));

        // snapshot taken before the stop-list change arrived
        cache.update(List.of(item(1L, false), item(2L, false)), fetchStartedAt);

        assertThat(cache.get(1L).orElseThrow().stopped()).isTrue();
        assertThat(cache.get(2L)).isPresent();
    }

    @Test
    void removalByEventSurvivesAnOlderRefresh() {
        long fetchStartedAt = System.currentTimeMillis() - 1000;
        cache.remove(1L);

        cache.update(List.of(item(1L, false)), fetchStartedAt);

        assertThat(cache.get(1L)).isEmpty();
    }

    @Test
    void refreshStartedAfterTheEventWins() throws InterruptedException {
        cache.upsert(item(1L, true));
        Thread.sleep(5);
        long fetchStartedAt = System.currentTimeMillis();

        cache.update(List.of(item(1L, false)), fetchStartedAt);

        assertThat(cache.get(1L).orElseThrow().stopped()).isFalse();
    }

    private static ItemPriceDto item(Long itemId, boolean stopped) {
        return new ItemPriceDto(itemId, 1L, "default", 3, "Item " + itemId, "DRINK", null, stopped,
                BigDecimal.valueOf(100), BigDecimal.valueOf(80), BigDecimal.valueOf(150));
    }
}
