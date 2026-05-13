package com.cryptocraft.inventoryservice.dao;

import com.cryptocraft.inventoryservice.config.converters.TypeConverter;
import com.cryptocraft.inventoryservice.dao.embedded.ItemProductId;
import com.cryptocraft.inventoryservice.dao.enums.Type;
import com.cryptocraft.inventoryservice.tenant.HasTenant;
import com.cryptocraft.inventoryservice.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@EntityListeners(TenantEntityListener.class)
@Table(
        name = "items",
        indexes = {
                @Index(name = "ix_items_tenant_id", columnList = "tenant_id"),
                @Index(name = "ix_items_tenant_name", columnList = "tenant_id,name")
        }
)
@ToString(onlyExplicitlyIncluded = true)
public class Item extends AuditingEntity implements HasTenant, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    @ToString.Include
    private Long itemId;

    @Column(nullable = false)
    @ToString.Include
    private String name;

    private String recipe;
    private String image;

    @Column(name = "type", nullable = false)
    @Convert(converter = TypeConverter.class)
    private Type type;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY, orphanRemoval = true,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<ItemProduct> productLinks = new ArrayList<>();

    public void addProduct(Product product) {
        addProduct(product, 1);
    }

    public void addProduct(Product product, int quantity) {
        if (product == null) throw new IllegalArgumentException("product is null");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");

        productLinks.stream()
                .filter(l -> product.equals(l.getProduct()))
                .findFirst()
                .ifPresentOrElse(
                        link -> link.setQuantity(link.getQuantity() + quantity),
                        () -> {
                            ItemProduct link = new ItemProduct();
                            link.setItem(this);
                            link.setProduct(product);
                            link.setId(new ItemProductId()); // @EmbeddedId + @MapsId
                            link.setQuantity(quantity);
                            link.setTenantId(this.tenantId); // важливо для RLS/trigger
                            productLinks.add(link);
                            product.getItemLinks().add(link);
                        }
                );
    }

    public void removeProduct(Product product) {
        if (product == null) return;
        productLinks.removeIf(link -> product.equals(link.getProduct()));
        product.getItemLinks().removeIf(link -> this.equals(link.getItem()));
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
        Item that = (Item) o;
        return itemId != null && Objects.equals(itemId, that.itemId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}