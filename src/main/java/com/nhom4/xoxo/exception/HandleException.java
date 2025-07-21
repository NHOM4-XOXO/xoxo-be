package com.nhom4.xoxo.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.nhom4.xoxo.constant.WrapResStatus;
import com.nhom4.xoxo.dto.WrapRes;

import io.jsonwebtoken.JwtException;

@RestControllerAdvice
public class HandleException extends ResponseEntityExceptionHandler {
        @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Object> handleServiceException(ServiceException ex, WebRequest request) {
        System.out.println("HandleException.handleServiceException");
        return handleExceptionInternal(ex, WrapRes.error(WrapResStatus.SERVICE_ERROR,ex.getMessage()), new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex, WebRequest request) {
        System.out.println("HandleException.handleNotFoundException");
        return handleExceptionInternal(ex, WrapRes.error(WrapResStatus.NOT_FOUND,ex.getMessage()), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        System.out.println("HandleException.handleUnauthorizedException");
        return handleExceptionInternal(ex, WrapRes.error(WrapResStatus.UNAUTHORIZED,ex.getMessage()), new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Object> handleJwtException(JwtException ex, WebRequest request) {
        System.out.println("HandleException.handleJwtException");
        return handleExceptionInternal(ex, WrapRes.error(WrapResStatus.SECURITY_ERROR, ex.getMessage()),
                new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

}
