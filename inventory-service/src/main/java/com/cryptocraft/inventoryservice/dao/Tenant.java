package com.cryptocraft.inventoryservice.dao;

import com.cryptocraft.inventoryservice.config.converters.TenantStatusConverter;
import com.cryptocraft.inventoryservice.dao.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.Objects;

/**
 * A venue (bar/restaurant). Not tenant-scoped itself: the tenants table has no RLS.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "tenants")
@ToString(onlyExplicitlyIncluded = true)
public class Tenant extends AuditingEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_id")
    @ToString.Include
    private Long tenantId;

    @Column(nullable = false)
    @ToString.Include
    private String name;

    // public identifier for URLs, QR codes and TV screens; never changes after creation
    @Column(nullable = false, unique = true, updatable = false)
    @ToString.Include
    private String slug;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "status", nullable = false)
    @Convert(converter = TenantStatusConverter.class)
    private TenantStatus status;

    @Column(name = "price_tick_seconds", nullable = false)
    private Integer priceTickSeconds;

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
        Tenant that = (Tenant) o;
        return tenantId != null && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
