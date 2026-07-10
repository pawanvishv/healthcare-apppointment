package com.healthapp.backend.adapter.in.web.advice;

import com.healthapp.backend.application.dto.ApiError;
import com.healthapp.backend.application.dto.ApiResponse;
import com.healthapp.backend.domain.exception.DomainException;
import com.healthapp.backend.domain.exception.IllegalStateTransitionException;
import com.healthapp.backend.domain.exception.ResourceNotFoundException;
import com.healthapp.backend.domain.exception.SlotAlreadyBookedException;
import com.healthapp.backend.domain.exception.UnauthorizedAccessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
                                                               HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Validation failed", request.getRequestURI(), details);
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex,
                                                           HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Invalid credentials", request.getRequestURI(), null);
    }

    @ExceptionHandler({AccessDeniedException.class, UnauthorizedAccessException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(Exception ex,
                                                              HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "Not authorized", request.getRequestURI(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex,
                                                            HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getErrorCode(),
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler({SlotAlreadyBookedException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> handleConflict(Exception ex,
                                                            HttpServletRequest request) {
        String message = ex instanceof SlotAlreadyBookedException
                ? ex.getMessage() : "Slot no longer available";
        return buildError(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE",
                message, request.getRequestURI(), null);
    }

    @ExceptionHandler({IllegalStateTransitionException.class})
    public ResponseEntity<ApiResponse<Void>> handleStateTransition(IllegalStateTransitionException ex,
                                                                    HttpServletRequest request) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getErrorCode(),
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomain(DomainException ex,
                                                           HttpServletRequest request) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case "EMAIL_EXISTS", "SLOT_UNAVAILABLE" -> HttpStatus.CONFLICT;
            case "VALIDATION_ERROR", "SLOT_IN_PAST", "SLOT_DOCTOR_MISMATCH" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return buildError(status, ex.getErrorCode(), ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request.getRequestURI(), null);
    }

    private ResponseEntity<ApiResponse<Void>> buildError(HttpStatus status, String errorCode,
                                                          String message, String path,
                                                          List<String> details) {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .path(path)
                .status(status.value())
                .errorCode(errorCode)
                .message(message)
                .traceId(traceId)
                .details(details)
                .build();

        return ResponseEntity.status(status).body(ApiResponse.fail(error));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
