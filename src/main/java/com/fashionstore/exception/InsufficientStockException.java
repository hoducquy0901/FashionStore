package com.fashionstore.exception;

public class InsufficientStockException extends OrderException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
