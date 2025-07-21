package com.nhom4.xoxo.dto;

import lombok.Data;

@Data
public class BaseAPI {
    private String message;
    private Object data;
    private boolean success;
    private int status;
    private String error;
}
