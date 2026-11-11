package com.cryptocraft.inventoryservice.repositories;

import com.cryptocraft.inventoryservice.controllers.dto.response.ItemPriceResponceDto;
import com.cryptocraft.inventoryservice.dao.enums.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Cross-tenant price configuration for the pricing service. Reads through the SECURITY DEFINER
 * function internal_item_prices(), because the items table itself is filtered by RLS per tenant.
 */
@Repository
@RequiredArgsConstructor
public class ItemPriceRepository {

    private static final String SELECT_PRICES = """
            SELECT item_id, tenant_id, tenant_slug, price_tick_seconds, name, type,
                   display_order, stopped, base_price, min_price, max_price
            FROM internal_item_prices()
            """;

    private static final RowMapper<ItemPriceResponceDto> ROW_MAPPER = (rs, rowNum) -> new ItemPriceResponceDto(
            rs.getLong("item_id"),
            rs.getLong("tenant_id"),
            rs.getString("tenant_slug"),
            rs.getInt("price_tick_seconds"),
            rs.getString("name"),
            typeOf(rs.getObject("type", Integer.class)),
            rs.getObject("display_order", Integer.class),
            rs.getBoolean("stopped"),
            rs.getBigDecimal("base_price"),
            rs.getBigDecimal("min_price"),
            rs.getBigDecimal("max_price"));

    private final JdbcClient jdbcClient;

    public List<ItemPriceResponceDto> findAllPrices() {
        return jdbcClient.sql(SELECT_PRICES)
                .query(ROW_MAPPER)
                .list();
    }

    // empty when the item is gone or its venue gets no live prices (not ACTIVE)
    public Optional<ItemPriceResponceDto> findByItemId(Long itemId) {
        return jdbcClient.sql(SELECT_PRICES + " WHERE item_id = ?")
                .param(itemId)
                .query(ROW_MAPPER)
                .optional();
    }

    // items.type is nullable in the schema
    private static Type typeOf(Integer id) {
        return id == null ? null : Type.byId(id);
    }
}
