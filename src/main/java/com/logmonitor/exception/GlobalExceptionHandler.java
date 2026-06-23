package com.logmonitor.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler returning standardized JSON error responses.
 *
 * <p>All API errors follow the format: {@code { timestamp, status, message, path }}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles invalid login credentials.
     *
     * @param ex      the credentials exception
     * @param request the web request
     * @return 401 response
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailure(
            Exception ex, WebRequest request) {

        ApiErrorResponse error = ApiErrorResponse.of(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid username or password",
                extractPath(request)
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles access denied (insufficient role).
     *
     * @param ex      access denied exception
     * @param request the web request
     * @return 403 response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        ApiErrorResponse error = ApiErrorResponse.of(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Access denied",
                extractPath(request)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles bean validation failures on request DTOs.
     *
     * @param ex      the validation exception
     * @param request the web request
     * @return 400 response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        ApiErrorResponse error = ApiErrorResponse.of(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                message,
                extractPath(request)
        );
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles missing resources.
     *
     * @param ex      the not-found exception
     * @param request the web request
     * @return 404 response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        ApiErrorResponse error = ApiErrorResponse.of(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                extractPath(request)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles non-whitelisted command keys and invalid search terms.
     *
     * @param ex      the exception
     * @param request the web request
     * @return 400 response
     */
    @ExceptionHandler({
            CommandNotWhitelistedException.class,
            InvalidSearchTermException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            RuntimeException ex, WebRequest request) {

        ApiErrorResponse error = ApiErrorResponse.of(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                extractPath(request)
        );
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles SSH connection and command failures.
     *
     * @param ex      the SSH exception
     * @param request the web request
     * @return 502 response
     */
    @ExceptionHandler({SshOperationException.class, SshConfigurationException.class})
    public ResponseEntity<ApiErrorResponse> handleSshFailure(
            RuntimeException ex, WebRequest request) {

        log.warn("SSH operation failed on {}: {}", extractPath(request), ex.getMessage());

        ApiErrorResponse error = ApiErrorResponse.of(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                ex.getMessage(),
                extractPath(request)
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    /**
     * Handles all unhandled exceptions.
     *
     * @param ex      the exception
     * @param request the web request
     * @return 500 response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Unhandled exception on {}: {}", extractPath(request), ex.getMessage(), ex);

        ApiErrorResponse error = ApiErrorResponse.of(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                extractPath(request)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
