package com.cryptocraft.orderservice.dao;

import com.cryptocraft.orderservice.config.converters.SessionStatusConverter;
import com.cryptocraft.orderservice.dao.enums.SessionStatus;
import com.cryptocraft.tenant.HasTenant;
import com.cryptocraft.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A visit at a table: opened by the first guest who scans the table's QR code, closed when the bill
 * is settled. Only one open session per table (partial unique index).
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@EntityListeners(TenantEntityListener.class)
@Table(name = "table_sessions")
@ToString(onlyExplicitlyIncluded = true)
public class TableSession extends AuditingEntity implements HasTenant, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id")
    @ToString.Include
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private VenueTable table;

    @Column(name = "status", nullable = false)
    @Convert(converter = SessionStatusConverter.class)
    @ToString.Include
    private SessionStatus status;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SessionParticipant> participants = new ArrayList<>();

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
        TableSession that = (TableSession) o;
        return sessionId != null && Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
