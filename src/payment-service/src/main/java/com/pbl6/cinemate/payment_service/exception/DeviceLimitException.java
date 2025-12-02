package com.pbl6.cinemate.payment_service.exception;

public class DeviceLimitException extends RuntimeException {
    
    public DeviceLimitException(String message) {
        super(message);
    }
}
