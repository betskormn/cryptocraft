package com.cryptocraft.inventoryservice.dao.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ItemProductId implements Serializable {

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemProductId that)) return false;
        return Objects.equals(itemId, that.itemId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, productId);
    }
}