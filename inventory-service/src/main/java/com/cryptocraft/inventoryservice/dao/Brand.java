package com.cryptocraft.inventoryservice.dao;

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
@Table(
        name = "brands",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_brands_name", columnNames = "name")
        }
)
@ToString(onlyExplicitlyIncluded = true)
public class Brand extends AuditingEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id")
    @ToString.Include
    private Long brandId;

    @Column(nullable = false)
    @ToString.Include
    private String name;

    @OneToMany(
            mappedBy = "brand",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            orphanRemoval = true

    )
    @Builder.Default
    private List<Product> products = new ArrayList<>();


    public void addProduct(Product p) {
        products.add(p);
        p.setBrand(this);
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