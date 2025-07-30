package com.nhom4.xoxo.exception;

public class PostException extends RuntimeException {
    
    public PostException(String message) {
        super(message);
    }
    
    public PostException(String message, Throwable cause) {
        super(message, cause);
    }
} 