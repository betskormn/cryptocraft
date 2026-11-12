package com.cryptocraft.inventoryservice.dao;

import com.cryptocraft.support.jpa.AuditingEntity;
import com.cryptocraft.tenant.HasTenant;
import com.cryptocraft.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@EntityListeners(TenantEntityListener.class)
@Table(
        name = "brands",
        indexes = {
                @Index(name = "uk_brands_tenant_name", columnList = "tenant_id,name", unique = true)
        }
)
@ToString(onlyExplicitlyIncluded = true)
public class Brand extends AuditingEntity implements HasTenant, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id")
    @ToString.Include
    private Long brandId;

    @Column(nullable = false)
    @ToString.Include
    private String name;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @OneToMany(
            mappedBy = "brand",
            fetch = FetchType.LAZY,
            // no REMOVE/orphanRemoval: deleting a brand must never delete its products (see BrandServiceImpl)
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    public void addProduct(Product p) {
        products.add(p);
        p.setBrand(this);
        if (p.getTenantId() == null) {
            p.setTenantId(this.tenantId);
        }
    }

    public void removeProduct(Product p) {
        products.remove(p);
        p.setBrand(null);
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
        Brand that = (Brand) o;
        return brandId != null && Objects.equals(brandId, that.brandId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}