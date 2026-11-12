package com.cryptocraft.identityservice.dao;

import com.cryptocraft.security.VenueRole;
import com.cryptocraft.support.jpa.AuditingEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A person works in a venue with one role. The venue itself lives in Inventory; only its id is kept here.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "venue_memberships")
@ToString(onlyExplicitlyIncluded = true)
public class VenueMembership extends AuditingEntity implements Serializable {

    @EmbeddedId
    @ToString.Include
    private Id id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private StaffUser user;

    @Column(nullable = false)
    @ToString.Include
    private VenueRole role;

    public Long getTenantId() {
        return id.getTenantId();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static class Id implements Serializable {

        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "tenant_id")
        private Long tenantId;
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
        VenueMembership that = (VenueMembership) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
