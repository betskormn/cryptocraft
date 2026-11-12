package com.cryptocraft.inventoryservice.dao;

import com.cryptocraft.support.jpa.AuditingEntity;
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

/**
 * Stock written off by recipe for one order. The unique (tenant_id, order_id) makes a repeated
 * request for the same order a no-op.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@EntityListeners(TenantEntityListener.class)
@Table(name = "stock_consumptions")
@ToString(onlyExplicitlyIncluded = true)
public class StockConsumption extends AuditingEntity implements HasTenant, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consumption_id")
    @ToString.Include
    private Long consumptionId;

    @Column(name = "order_id", nullable = false, updatable = false)
    @ToString.Include
    private String orderId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @OneToMany(mappedBy = "consumption", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @Builder.Default
    private List<StockConsumptionLine> lines = new ArrayList<>();

    public void addLine(Product product, BigDecimal requested, BigDecimal consumed) {
        StockConsumptionLine line = new StockConsumptionLine();
        line.setConsumption(this);
        line.setProductId(product.getProductId());
        line.setProductName(product.getName());
        line.setRequested(requested);
        line.setConsumed(consumed);
        line.setTenantId(this.tenantId);
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
        StockConsumption that = (StockConsumption) o;
        return consumptionId != null && Objects.equals(consumptionId, that.consumptionId);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
