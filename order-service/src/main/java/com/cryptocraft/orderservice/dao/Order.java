package com.cryptocraft.orderservice.dao;

import com.cryptocraft.orderservice.config.converters.OrderStatusConverter;
import com.cryptocraft.orderservice.config.converters.StockStatusConverter;
import com.cryptocraft.orderservice.dao.enums.OrderStatus;
import com.cryptocraft.orderservice.dao.enums.StockStatus;
import com.cryptocraft.tenant.HasTenant;
import com.cryptocraft.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Items a guest sent to the kitchen/bar at once. Prices are locked at that moment (order_lines) and
 * added to the guest's bill; the bill is paid at the end of the visit.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@EntityListeners(TenantEntityListener.class)
@Table(name = "orders")
@ToString(onlyExplicitlyIncluded = true)
public class Order extends AuditingEntity implements HasTenant, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    @ToString.Include
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TableSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private SessionParticipant participant;

    // the client's idempotency key: resending the same order returns the first one
    @Column(name = "client_order_id", nullable = false, updatable = false)
    @ToString.Include
    private String clientOrderId;

    @Column(name = "status", nullable = false)
    @Convert(converter = OrderStatusConverter.class)
    private OrderStatus status;

    @Column(name = "stock_status", nullable = false)
    @Convert(converter = StockStatusConverter.class)
    private StockStatus stockStatus;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @OrderBy("lineId")
    @Builder.Default
    private List<OrderLine> lines = new ArrayList<>();

    public void addLine(OrderLine line) {
        line.setOrder(this);
        lines.add(line);
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
        Order that = (Order) o;
        return orderId != null && Objects.equals(orderId, that.orderId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
