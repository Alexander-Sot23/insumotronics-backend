package com.alexander.springboot.insumotronics.exception;

public class ReserveNotFoundException extends RuntimeException {
    public ReserveNotFoundException(String message) {
        super(message);
    }
}

