package com.healthapp.backend.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private Instant timestamp;
    private String path;
    private int status;
    private String errorCode;
    private String message;
    private String traceId;
    private List<String> details;
}
