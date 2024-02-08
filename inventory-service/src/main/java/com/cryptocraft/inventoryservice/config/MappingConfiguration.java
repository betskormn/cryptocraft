package com.cryptocraft.inventoryservice.config;

import com.cryptocraft.inventoryservice.config.converters.InstantToDateConverter;
import com.cryptocraft.inventoryservice.controllers.dto.request.BrandRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.request.ProductRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.BrandResponseDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ProductResponseDto;
import com.cryptocraft.inventoryservice.dao.Brand;
import com.cryptocraft.inventoryservice.dao.Product;
import ma.glasnost.orika.*;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.ClassMapBuilder;
import ma.glasnost.orika.metadata.Type;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MappingConfiguration {

    private static final String BOOLEAN_TO_INTEGER_CONVERTER = "booleanToIntegerConverter";


    @Bean
    Mapper<ProductRequestDto, Product> productRequestDtoToProductMapper(final MapperFactory factory) {
        return toRegisteredMapper(factory.classMap(ProductRequestDto.class, Product.class)
                .mapNulls(true)
                .field("quantity", "quantityInStock")
                .byDefault());
    }

    @Bean
    Mapper<Product, ProductResponseDto> productToProductResponseDtoMapper(final MapperFactory factory) {
        return toRegisteredMapper(factory.classMap(Product.class, ProductResponseDto.class)
                .customize(new CustomMapper<>() {
                    @Override
                    public void mapAtoB(Product product, ProductResponseDto responseDto, MappingContext ctx) {
//                        responseDto.setRecipeName(product.getRecipeName());
                    }
                })
                .byDefault());
    }

    @Bean
    Mapper<Brand, BrandResponseDto> brandToBrandResponseDtoMapper(final MapperFactory factory) {
        return toRegisteredMapper(factory.classMap(Brand.class, BrandResponseDto.class)
                .byDefault());
    }

    @Bean
    Mapper<BrandRequestDto, Brand> brandRequestDtoToBrandtMapper(final MapperFactory factory) {
        return toRegisteredMapper(factory.classMap(BrandRequestDto.class, Brand.class)
                .mapNulls(true)
                .byDefault());
    }

    private <A, B> Mapper<A, B> toRegisteredMapper(final ClassMapBuilder<A, B> builder) {
        builder.register();
        return builder.toClassMap().getCustomizedMapper();
    }


    @Bean
    MapperFactory mapperFactory() {
        final DefaultMapperFactory defaultMapperFactory = new DefaultMapperFactory.Builder().mapNulls(false).build();
        final ConverterFactory converterFactory = defaultMapperFactory.getConverterFactory();
        converterFactory.registerConverter(BOOLEAN_TO_INTEGER_CONVERTER, booleanToIntegerConverter());
        converterFactory.registerConverter(new InstantToDateConverter());
        return defaultMapperFactory;
    }

    @Bean
    Converter<Boolean, Integer> booleanToIntegerConverter() {
        return new CustomConverter<>() {
            @Override
            public Integer convert(Boolean source, Type<? extends Integer> destinationType, MappingContext mappingContext) {
                return BooleanUtils.isTrue(source) ? 1 : 0;
            }
        };
    }

    @Bean
    MapperFacade mapperFacade(final MapperFactory factory) {
        return factory.getMapperFacade();
    }
}