package com.minuStore.MiNu.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponseDto {

    private LocalDateTime timestamp;
    private int status;
    private String message;
    private String path;
}