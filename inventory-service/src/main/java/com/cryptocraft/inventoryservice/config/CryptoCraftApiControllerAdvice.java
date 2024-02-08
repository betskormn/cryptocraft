package com.cryptocraft.inventoryservice.config;

import com.cryptocraft.inventoryservice.exceptions.ExceptionGlossary;
import com.cryptocraft.inventoryservice.exceptions.ExceptionHandlerMessageUtil;
import com.cryptocraft.inventoryservice.exceptions.GlobalErrorResponseDto;
import com.cryptocraft.inventoryservice.exceptions.StackTraceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@ControllerAdvice
public class CryptoCraftApiControllerAdvice {
    @ExceptionHandler(value = {Throwable.class})
    protected ResponseEntity<Object> handleException(Throwable ex) {
        List<StackTraceDto> stackTrace = Stream.of(ex.getStackTrace())
                .filter(it -> it.getClassName().startsWith("com.cryptocraft"))
                .map(StackTraceDto::new)
                .collect(Collectors.toList());
        ExceptionHandlerMessageUtil exceptionHandlerMessageUtil = new ExceptionHandlerMessageUtil();
        List<String> list = exceptionHandlerMessageUtil.composeMessage(ex);
        GlobalErrorResponseDto globalErrorResponseDto = new GlobalErrorResponseDto(list,
                ex.getLocalizedMessage(), stackTrace);
        return new ResponseEntity<>(globalErrorResponseDto, ExceptionGlossary.getCodeByException(ex.getClass()));
    }
}
