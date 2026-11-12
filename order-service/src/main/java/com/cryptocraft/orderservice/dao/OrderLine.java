package com.cryptocraft.orderservice.dao;

import com.cryptocraft.orderservice.config.converters.ItemTypeConverter;
import com.cryptocraft.orderservice.config.converters.LineStatusConverter;
import com.cryptocraft.orderservice.dao.enums.ItemType;
import com.cryptocraft.orderservice.dao.enums.LineStatus;
import com.cryptocraft.tenant.HasTenant;
import com.cryptocraft.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * One item of an order with its price locked when the order was sent. Name and type are copied, so
 * the bill and the kitchen screen do not depend on later menu changes. The kitchen/bar moves the line
 * through {@link LineStatus}.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@EntityListeners(TenantEntityListener.class)
@Table(name = "order_lines")
@ToString(onlyExplicitlyIncluded = true)
public class OrderLine implements HasTenant, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "item_id", nullable = false)
    @ToString.Include
    private Long itemId;

    @Column(name = "item_name", nullable = false)
    @ToString.Include
    private String itemName;

    @Column(name = "item_type", nullable = false)
    @Convert(converter = ItemTypeConverter.class)
    private ItemType itemType;

    @Column(nullable = false)
    @ToString.Include
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    // kitchen/bar screen state; a CANCELLED line is not charged
    @Column(nullable = false)
    @Convert(converter = LineStatusConverter.class)
    @ToString.Include
    @Builder.Default
    private LineStatus status = LineStatus.NEW;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

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
        OrderLine that = (OrderLine) o;
        return lineId != null && Objects.equals(lineId, that.lineId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
