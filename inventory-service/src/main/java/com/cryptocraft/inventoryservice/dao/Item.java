package com.cryptocraft.inventoryservice.dao;

import com.cryptocraft.inventoryservice.config.converters.TypeConverter;
import com.cryptocraft.inventoryservice.dao.enums.Type;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@ToString
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
    @OneToMany(mappedBy = "productId", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(value = FetchMode.JOIN)
    private List<Product> products;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(itemId, item.itemId) && Objects.equals(name, item.name) && Objects.equals(recipe, item.recipe) && Objects.equals(image, item.image) && Objects.equals(type, item.type) && Objects.equals(products, item.products);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, name, recipe, image, type, products);
    }
}
