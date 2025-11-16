package com.pbl6.cinemate.payment_service.exception;

public class InvalidPaymentException extends RuntimeException {
    
    public InvalidPaymentException(String message) {
        super(message);
    }
}
