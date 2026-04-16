package ru.practicum.ewm.error;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.ewm.dto.ApiError;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Validation error: {}", e.getMessage(), e);
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> "Field: " + error.getField() + ". Error: " + error.getDefaultMessage())
                .toList();
        return ApiError.builder()
                .errors(errors)
                .message(e.getMessage())
                .reason("Incorrectly made request.")
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler({ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolationException(ConstraintViolationException e) {
        log.error("Constraint violation: {}", e.getMessage(), e);
        List<String> errors = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return ApiError.builder()
                .errors(errors)
                .message(e.getMessage())
                .reason("Incorrectly made request.")
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class, EventDateValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(Exception e) {
        log.error("Bad request: {}", e.getMessage(), e);
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message(e.getMessage())
                .reason("Incorrectly made request.")
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler({DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("Data integrity violation: {}", e.getMessage(), e);
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message(e.getMessage())
                .reason("Integrity constraint has been violated.")
                .status("CONFLICT")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler({ConflictException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException e) {
        log.error("Conflict: {}", e.getMessage(), e);
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message(e.getMessage())
                .reason("For the requested operation the conditions are not met.")
                .status("CONFLICT")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler({NotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(NotFoundException e) {
        log.error("Not found: {}", e.getMessage(), e);
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message(e.getMessage())
                .reason("The required object was not found.")
                .status("NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler({ForbiddenException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleForbiddenException(ForbiddenException e) {
        log.error("Forbidden: {}", e.getMessage(), e);
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message(e.getMessage())
                .reason("Access denied.")
                .status("FORBIDDEN")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler({RuntimeException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(RuntimeException e) {
        log.error("Not found: {}", e.getMessage(), e);
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message(e.getMessage())
                .reason("The required object was not found.")
                .status("NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleInternalServerError(Exception e) {
        log.error("Internal server error: {}", e.getMessage(), e);
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message(e.getMessage())
                .reason("Internal server error.")
                .status("INTERNAL_SERVER_ERROR")
                .timestamp(LocalDateTime.now())
                .build();
    }
}