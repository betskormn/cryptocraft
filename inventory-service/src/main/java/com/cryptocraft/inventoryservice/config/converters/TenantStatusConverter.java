package com.cryptocraft.inventoryservice.config.converters;

import com.cryptocraft.inventoryservice.dao.enums.TenantStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class TenantStatusConverter implements AttributeConverter<TenantStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(TenantStatus attribute) {
        return attribute == null ? null : attribute.id().shortValue();
    }

    @Override
    public TenantStatus convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : TenantStatus.byId(dbData.intValue());
    }
}
