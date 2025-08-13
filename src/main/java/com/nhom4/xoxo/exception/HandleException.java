package com.nhom4.xoxo.exception;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.exception.GenericJDBCException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.nhom4.xoxo.constant.WrapResStatus;
import com.nhom4.xoxo.dto.WrapRes;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class HandleException extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Object> handleServiceException(ServiceException ex, WebRequest request) {
        log.error("HandleException.handleServiceException");
        return handleExceptionInternal(ex, WrapRes.error(WrapResStatus.SERVICE_ERROR, ex.getMessage()),
                new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex, WebRequest request) {
        log.error("HandleException.handleNotFoundException");
        return handleExceptionInternal(ex, WrapRes.error(WrapResStatus.NOT_FOUND, ex.getMessage()), new HttpHeaders(),
                HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        log.error("HandleException.handleUnauthorizedException");
        return handleExceptionInternal(ex, WrapRes.error(WrapResStatus.UNAUTHORIZED, ex.getMessage()),
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        log.error("HandleException.handleForbiddenException", ex);
        return handleExceptionInternal(ex, WrapRes.error(WrapResStatus.SECURITY_ERROR, ex.getMessage()),
                new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(PostException.class)
    public ResponseEntity<Object> handlePostException(PostException ex, WebRequest request) {
        log.error("HandleException.handlePostException", ex);
        return handleExceptionInternal(ex, WrapRes.error(WrapResStatus.BAD_REQUEST, ex.getMessage()),
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Object> handleJwtException(JwtException ex, WebRequest request) {
        log.error("HandleException.handleJwtException");
        return handleExceptionInternal(ex, WrapRes.error(WrapResStatus.SECURITY_ERROR, ex.getMessage()),
                new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull org.springframework.http.converter.HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        String message = "Request body không hợp lệ hoặc thiếu dữ liệu.";
        Throwable cause = ex.getCause();
        if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException) {
            com.fasterxml.jackson.databind.exc.InvalidFormatException ife = (com.fasterxml.jackson.databind.exc.InvalidFormatException) cause;
            // Kiểm tra nếu lỗi là do enum
            if (ife.getTargetType().isEnum() && !ife.getPath().isEmpty()) {
                String field = ife.getPath().get(0).getFieldName();
                Object[] enumConstants = ife.getTargetType().getEnumConstants();
                String allowed = "";
                if (enumConstants != null) {
                    allowed = java.util.Arrays.toString(enumConstants);
                }
                message = "Trường '" + field + "' chỉ được phép là: " + allowed.replaceAll("[\\[\\]]", "");
            }
        }
        return handleExceptionInternal(
                ex,
                WrapRes.error(WrapResStatus.BAD_REQUEST, message),
                headers,
                HttpStatus.BAD_REQUEST,
                request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        return handleExceptionInternal(
                ex,
                WrapRes.error(WrapResStatus.BAD_REQUEST, errors.toString()),
                headers,
                HttpStatus.BAD_REQUEST,
                request);
    }

   

    @ExceptionHandler(GenericJDBCException.class)
    public ResponseEntity<?> handleGenericJDBCException(GenericJDBCException ex) {
        String message = ex.getSQLException().getMessage();
        if (message != null && message.contains("Chỉ user gốc mới được phép có role OWNER")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(WrapRes.error(WrapResStatus.BAD_REQUEST, "Chỉ user gốc mới được phép có role OWNER!"));
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(WrapRes.error(WrapResStatus.BAD_REQUEST, "Lỗi dữ liệu!"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        log.error("HandleException.handleGenericException", ex);
        return handleExceptionInternal(ex, 
                WrapRes.error(WrapResStatus.SERVICE_ERROR, "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau."),
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            @NonNull MaxUploadSizeExceededException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        log.warn("HandleException.handleMaxUploadSizeExceededException: {}", ex.getMessage());
        String message = "Kích thước file vượt quá giới hạn cho phép.";
        return handleExceptionInternal(
                ex,
                WrapRes.error(WrapResStatus.BAD_REQUEST, message),
                headers,
                HttpStatus.PAYLOAD_TOO_LARGE,
                request);
    }
}
