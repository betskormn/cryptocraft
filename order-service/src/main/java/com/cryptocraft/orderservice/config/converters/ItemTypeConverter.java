package com.cryptocraft.orderservice.config.converters;

import com.cryptocraft.orderservice.dao.enums.ItemType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class ItemTypeConverter implements AttributeConverter<ItemType, Short> {

    @Override
    public Short convertToDatabaseColumn(ItemType attribute) {
        return attribute == null ? null : attribute.id().shortValue();
    }

    @Override
    public ItemType convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : ItemType.byId(dbData.intValue());
    }
}
