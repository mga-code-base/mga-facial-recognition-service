package com.mygym.app.facialrecognition.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String RETRY_AFTER_SECONDS = "10";

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadArgument(IllegalArgumentException ex, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request parameters are invalid", request);
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(DownstreamUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleDownstreamUnavailable(DownstreamUnavailableException ex, WebRequest request) {
        LOGGER.warn("Downstream {} unavailable: {}", ex.getDownstream(), ex.getMessage());
        ProblemDetail detail = problem(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable",
                "A dependent service is not responding, please retry shortly", request);
        detail.setProperty("downstream", ex.getDownstream());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(detail);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ProblemDetail> handleResourceAccess(ResourceAccessException ex, WebRequest request) {
        LOGGER.warn("Downstream I/O failure: {}", ex.getMessage());
        ProblemDetail detail = problem(HttpStatus.GATEWAY_TIMEOUT, "Downstream timeout",
                "A dependent service did not respond in time", request);
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(detail);
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ProblemDetail handleDownstreamError(RestClientResponseException ex, WebRequest request) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.is5xxServerError()) {
            LOGGER.error("Downstream returned {}", status.value());
            return problem(HttpStatus.BAD_GATEWAY, "Downstream error", "A dependent service failed", request);
        }
        return problem(status, "Downstream rejected the request", ex.getStatusText(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", "You do not have access to this resource", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, WebRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication is required", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
        LOGGER.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "An unexpected error occurred", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request body is invalid", request);
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(detail);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request is invalid", request);
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> result.getResolvableErrors()
                .forEach(error -> errors.putIfAbsent(result.getMethodParameter().getParameterName(), error.getDefaultMessage())));
        detail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(detail);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail detail) {
            enrich(detail, request);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ProblemDetail problem(HttpStatusCode status, String title, String detailMessage, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, detailMessage);
        detail.setTitle(title);
        enrich(detail, request);
        return detail;
    }

    private void enrich(ProblemDetail detail, WebRequest request) {
        if (request instanceof ServletWebRequest servlet) {
            detail.setInstance(URI.create(servlet.getRequest().getRequestURI()));
        }
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            detail.setProperty("traceId", traceId);
        }
    }
}
