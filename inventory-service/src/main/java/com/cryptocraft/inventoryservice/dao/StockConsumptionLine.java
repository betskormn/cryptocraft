package com.cryptocraft.inventoryservice.dao;

import com.cryptocraft.tenant.HasTenant;
import com.cryptocraft.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One product of a consumption. {@code consumed} is less than {@code requested} when the recorded
 * stock ran out; the product name is kept so the history survives the product being deleted.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@EntityListeners(TenantEntityListener.class)
@Table(
        name = "stock_consumption_lines",
        indexes = {
                @Index(name = "ix_stock_consumption_lines_consumption", columnList = "consumption_id")
        }
)
public class StockConsumptionLine implements HasTenant, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumption_id", nullable = false)
    private StockConsumption consumption;

    // plain id, not an association: set to null by the database when the product is deleted
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private BigDecimal requested;

    @Column(nullable = false)
    private BigDecimal consumed;

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
        StockConsumptionLine that = (StockConsumptionLine) o;
        return lineId != null && Objects.equals(lineId, that.lineId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
