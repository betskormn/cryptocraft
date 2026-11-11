package com.cryptocraft.orderservice.scheduler;

import com.cryptocraft.orderservice.services.StockWriteOffService;
import com.cryptocraft.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Write-offs that failed when the order was placed (Inventory down) are retried here. Orders of all venues
 * are found through the SECURITY DEFINER function pending_stock_write_offs(), RLS hides them otherwise;
 * each one is then handled within its own venue.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockWriteOffRetrier {

    private final JdbcClient jdbcClient;
    private final StockWriteOffService stockWriteOffService;

    // leaves the request-time attempt alone
    @Value("${order.stock-retry-min-age-seconds:10}")
    private int minAgeSeconds;

    @Scheduled(fixedDelayString = "${order.stock-retry-ms:30000}", initialDelayString = "${order.stock-retry-ms:30000}")
    public void retryPendingWriteOffs() {
        jdbcClient.sql("SELECT order_id, tenant_id FROM pending_stock_write_offs(?)")
                .param(minAgeSeconds)
                .query((rs, rowNum) -> new Pending(rs.getObject("order_id", UUID.class), rs.getLong("tenant_id")))
                .list()
                .forEach(this::retry);
    }

    private void retry(Pending pending) {
        try {
            TenantContext.set(pending.tenantId());
            stockWriteOffService.writeOff(pending.orderId());
        } finally {
            TenantContext.clear();
        }
    }

    private record Pending(UUID orderId, Long tenantId) {}
}
