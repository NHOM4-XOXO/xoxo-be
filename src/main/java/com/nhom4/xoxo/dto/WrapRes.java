package com.nhom4.xoxo.dto;

import com.nhom4.xoxo.constant.WrapResStatus;

import lombok.Data;

@Data
public class WrapRes <T>{
    private String statusCode;
    private String message;
    private T data;
    
    public static <T> WrapRes<T> success(T data) {
        WrapRes<T> res = new WrapRes<>();
        res.setStatusCode(WrapResStatus.SUCCESS);
        res.setMessage("Success");
        res.setData(data);
        return res;
    }
    
    public static <T> WrapRes<T> error( String message) {
        WrapRes<T> res = new WrapRes<>();
        res.data = null;
        res.setStatusCode(WrapResStatus.INTERNAL_SERVER_ERROR);
        res.setMessage(message);
        return res;
    }
    
    public static <T> WrapRes<T> error(String statusCode, String message) {
        WrapRes<T> res = new WrapRes<>();
        res.data = null;
        res.setStatusCode(statusCode);
        res.setMessage(message);
        return res;
    }
    
    public static <T> WrapRes<T> login(String token) {
        WrapRes<T> res = new WrapRes<>();
        res.setStatusCode(WrapResStatus.SUCCESS);
        res.setMessage(token);
        return res;
    }
}
