package com.nhom4.xoxo.exception;

import io.jsonwebtoken.JwtException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


public class JWTException extends JwtException {
    public JWTException(String message) {
        super(message);
    }
}