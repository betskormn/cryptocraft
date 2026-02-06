package com.cryptocraft.inventoryservice.dao;

import com.cryptocraft.inventoryservice.dao.embedded.ItemProductId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "item_products",
        indexes = {
                @Index(name = "ix_item_products_item_id", columnList = "item_id"),
                @Index(name = "ix_item_products_product_id", columnList = "product_id")
        }
)
public class ItemProduct implements Serializable {

    @EmbeddedId
    private ItemProductId id;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("itemId")
    @JoinColumn(name = "item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_products_item"))
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("productId")
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_products_product"))
    private Product product;

    public static ItemProduct of(Item item, Product product, int quantity) {
        ItemProduct link = new ItemProduct();
        link.setItem(item);
        link.setProduct(product);
        link.setId(new ItemProductId());
        link.setQuantity(quantity);
        return link;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();

        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();

        if (thisEffectiveClass != oEffectiveClass) return false;

        ItemProduct that = (ItemProduct) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}