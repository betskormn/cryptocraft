package com.cryptocraft.inventoryservice.config.converters;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

import java.time.Instant;
import java.util.Date;

public class InstantToDateConverter extends BidirectionalConverter<Instant, Date> {

    @Override
    public Date convertTo(Instant source, Type<Date> destinationType, MappingContext mappingContext) {
        if (source == null) {
            return null;
        }
        return Date.from(source);
    }

    @Override
    public Instant convertFrom(Date source, Type<Instant> destinationType, MappingContext mappingContext) {
        if (source == null) {
            return null;
        }
        return source.toInstant();
    }
}