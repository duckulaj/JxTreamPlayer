package com.hawkins.xtreamjson.util;

public class ProxyValidationException extends RuntimeException {
    private static final long serialVersionUID = -6773243453019732313L;
	private final int statusCode;

    public ProxyValidationException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
