package com.predicted.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Map<String, Object>> badRequest(BadRequestException exception) {
    return ResponseEntity.badRequest().body(error("BAD_REQUEST", exception.getMessage()));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NOT_FOUND", exception.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Map<String, Object>> conflict(ConflictException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error("CONFLICT", exception.getMessage()));
  }

  @ExceptionHandler(TooManyRequestsException.class)
  public ResponseEntity<Map<String, Object>> tooManyRequests(TooManyRequestsException exception) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()))
        .body(error("RATE_LIMITED", exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
    String fields = exception.getBindingResult()
        .getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
            (left, right) -> left
        ))
        .toString();
    return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", fields));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<Map<String, Object>> uploadTooLarge(MaxUploadSizeExceededException exception) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(error("UPLOAD_TOO_LARGE", "Uploaded file is larger than the configured limit."));
  }

  @ExceptionHandler(MultipartException.class)
  public ResponseEntity<Map<String, Object>> multipart(MultipartException exception) {
    return ResponseEntity.badRequest().body(error("MULTIPART_ERROR", "Upload request is not valid multipart data."));
  }

  private Map<String, Object> error(String code, String message) {
    return Map.of(
        "code", code,
        "message", message,
        "timestamp", Instant.now().toString()
    );
  }
}
