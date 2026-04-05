package com.minuStore.MiNu.exception;

import com.minuStore.MiNu.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.error("ResourceNotFoundException at {}: {}", request.getRequestURI(), ex.getMessage());
        if (isApiRequest(request)) {
            return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
        }
        return buildErrorView(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(com.minuStore.MiNu.exception.BadRequestException.class)
    public Object handleCustomBadRequest(com.minuStore.MiNu.exception.BadRequestException ex, HttpServletRequest request) {
        log.error("BadRequestException at {}: {}", request.getRequestURI(), ex.getMessage());
        if (isApiRequest(request)) {
            return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        }
        return buildErrorView(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public Object handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        log.error("BadRequestException (Tomcat) at {}: {}", request.getRequestURI(), ex.getMessage());
        if (isApiRequest(request)) {
            return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        }
        return buildErrorView(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.error("AccessDeniedException at {}: {}", request.getRequestURI(), ex.getMessage());
        if (isApiRequest(request)) {
            return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request);
        }
        return buildErrorView(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Validation failed";
        }

        log.error("Validation error at {}: {}", request.getRequestURI(), message);
        if (isApiRequest(request)) {
            return buildError(HttpStatus.BAD_REQUEST, message, request);
        }
        return buildErrorView(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at " + request.getRequestURI(), ex);
        if (isApiRequest(request)) {
            return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
        }
        return buildErrorView(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");
        return (accept != null && accept.contains("application/json")) ||
                (contentType != null && contentType.contains("application/json")) ||
                request.getRequestURI().startsWith("/api/");
    }

    private ResponseEntity<ErrorResponseDto> buildError(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }

    private ModelAndView buildErrorView(HttpStatus status, String message, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        mav.addObject("path", request.getRequestURI());
        mav.addObject("timestamp", LocalDateTime.now());
        mav.setStatus(status);
        return mav;
    }
}