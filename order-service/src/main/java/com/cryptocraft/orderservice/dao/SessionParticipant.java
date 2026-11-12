package com.cryptocraft.orderservice.dao;

import com.cryptocraft.orderservice.dao.enums.GuestStatus;
import com.cryptocraft.support.jpa.AuditingEntity;
import com.cryptocraft.tenant.HasTenant;
import com.cryptocraft.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A guest in a table session. Until Identity Service exists the participant id is the guest's
 * guest token (issued when joining the table): each guest orders and pays for themselves. A new guest's orders
 * wait until a waiter confirms the guest ({@link GuestStatus}).
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@EntityListeners(TenantEntityListener.class)
@Table(name = "session_participants")
@ToString(onlyExplicitlyIncluded = true)
public class SessionParticipant extends AuditingEntity implements HasTenant, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "participant_id")
    @ToString.Include
    private UUID participantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TableSession session;

    @Column(name = "display_name")
    @ToString.Include
    private String displayName;

    @Builder.Default
    @Column(nullable = false)
    @ToString.Include
    private GuestStatus status = GuestStatus.PENDING;

    // when a waiter confirmed or rejected the guest
    @Column(name = "decided_at")
    private Instant decidedAt;

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
        SessionParticipant that = (SessionParticipant) o;
        return participantId != null && Objects.equals(participantId, that.participantId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
