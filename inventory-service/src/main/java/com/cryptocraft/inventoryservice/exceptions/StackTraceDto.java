package com.cryptocraft.inventoryservice.exceptions;

import lombok.Data;

@Data
public class StackTraceDto {
    private String declaringClass;
    private Integer lineNumber;

    public StackTraceDto(StackTraceElement stackTraceElement) {
        this.declaringClass = stackTraceElement.getClassName();
        this.lineNumber = stackTraceElement.getLineNumber();
    }
}