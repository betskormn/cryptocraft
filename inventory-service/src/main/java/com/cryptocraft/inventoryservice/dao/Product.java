package com.cryptocraft.inventoryservice.dao;

import com.cryptocraft.inventoryservice.config.converters.UnitConverter;
import com.cryptocraft.inventoryservice.dao.enums.Unit;
import com.cryptocraft.inventoryservice.tenant.HasTenant;
import com.cryptocraft.inventoryservice.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.math.BigDecimal;
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
        name = "products",
        indexes = {
                @Index(name = "ix_products_brand_id", columnList = "brand_id"),
                @Index(name = "ix_products_tenant_id", columnList = "tenant_id")
        }
)
@ToString(onlyExplicitlyIncluded = true)
public class Product extends AuditingEntity implements HasTenant, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    @ToString.Include
    private Long productId;

    @Column(nullable = false)
    @ToString.Include
    private String name;

    private String description;
    private String image;

    @Column(name = "quantity_in_stock", nullable = false)
    // in the product's unit, up to 3 decimals; never below zero (DB check)
    private BigDecimal quantityInStock;

    @Column(name = "unit", nullable = false)
    @Convert(converter = UnitConverter.class)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_products_brand"))
    private Brand brand;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, orphanRemoval = true,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<ItemProduct> itemLinks = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void alignTenantWithBrand() {
        if (brand != null) {
            if (tenantId == null) {
                tenantId = brand.getTenantId();
            } else if (!tenantId.equals(brand.getTenantId())) {
                throw new IllegalStateException("Tenant mismatch: product.tenantId != brand.tenantId");
            }
        }
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
        Product that = (Product) o;
        return productId != null && Objects.equals(productId, that.productId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}