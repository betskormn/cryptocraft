package com.cryptocraft.orderservice.dao;

import com.cryptocraft.support.jpa.AuditingEntity;
import com.cryptocraft.tenant.HasTenant;
import com.cryptocraft.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.Objects;

/**
 * A table of the venue. {@code code} is the random token in the table's QR code: scanning it lets a guest
 * join the table's current visit.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@EntityListeners(TenantEntityListener.class)
@Table(name = "venue_tables")
@ToString(onlyExplicitlyIncluded = true)
public class VenueTable extends AuditingEntity implements HasTenant, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_id")
    @ToString.Include
    private Long tableId;

    @Column(nullable = false)
    @ToString.Include
    private String label;

    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

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
        VenueTable that = (VenueTable) o;
        return tableId != null && Objects.equals(tableId, that.tableId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
