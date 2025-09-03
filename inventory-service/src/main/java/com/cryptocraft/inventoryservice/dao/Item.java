package com.cryptocraft.inventoryservice.dao;

import com.cryptocraft.inventoryservice.config.converters.TypeConverter;
import com.cryptocraft.inventoryservice.dao.enums.Type;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@ToString(exclude = {"products"})
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "items")
public class Item extends AuditingEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false)
    private String name;
    private String recipe;
    private String image;
    @Convert(converter = TypeConverter.class)
    private Type type;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "item_products",
            joinColumns = @JoinColumn(name = "item_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> products = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;

        Item item = (Item) o;
        return itemId != null && Objects.equals(itemId, item.itemId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
