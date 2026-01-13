package com.hawkins.xtreamjson.util;

public class ProxyValidationException extends RuntimeException {
    private final int statusCode;

    public ProxyValidationException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
