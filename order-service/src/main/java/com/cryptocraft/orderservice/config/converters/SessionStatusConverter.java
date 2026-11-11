package com.cryptocraft.orderservice.config.converters;

import com.cryptocraft.orderservice.dao.enums.SessionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class SessionStatusConverter implements AttributeConverter<SessionStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(SessionStatus attribute) {
        return attribute == null ? null : attribute.id().shortValue();
    }

    @Override
    public SessionStatus convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : SessionStatus.byId(dbData.intValue());
    }
}
