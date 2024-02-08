package com.cryptocraft.inventoryservice.exceptions;

import lombok.Value;

import java.util.List;

@Value
public class GlobalErrorResponseDto {
    private final List<String> message;
    private final String description;
    private final List<StackTraceDto> stackTraceElements;
}