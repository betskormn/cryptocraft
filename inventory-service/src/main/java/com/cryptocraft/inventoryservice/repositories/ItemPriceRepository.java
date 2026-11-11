package com.cryptocraft.inventoryservice.repositories;

import com.cryptocraft.inventoryservice.controllers.dto.response.ItemPriceResponceDto;
import com.cryptocraft.inventoryservice.dao.enums.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Cross-tenant price configuration for the pricing service. Reads through the SECURITY DEFINER
 * function internal_item_prices(), because the items table itself is filtered by RLS per tenant.
 */
@Repository
@RequiredArgsConstructor
public class ItemPriceRepository {

    private final JdbcClient jdbcClient;

    public List<ItemPriceResponceDto> findAllPrices() {
        return jdbcClient.sql("""
                        SELECT item_id, tenant_id, tenant_slug, price_tick_seconds, name, type,
                               base_price, min_price, max_price
                        FROM internal_item_prices()
                        """)
                .query((rs, rowNum) -> new ItemPriceResponceDto(
                        rs.getLong("item_id"),
                        rs.getLong("tenant_id"),
                        rs.getString("tenant_slug"),
                        rs.getInt("price_tick_seconds"),
                        rs.getString("name"),
                        typeOf(rs.getObject("type", Integer.class)),
                        rs.getBigDecimal("base_price"),
                        rs.getBigDecimal("min_price"),
                        rs.getBigDecimal("max_price")))
                .list();
    }

    // items.type is nullable in the schema
    private static Type typeOf(Integer id) {
        return id == null ? null : Type.byId(id);
    }
}
