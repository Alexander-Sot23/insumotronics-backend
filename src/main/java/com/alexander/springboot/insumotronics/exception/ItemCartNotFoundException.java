package com.alexander.springboot.insumotronics.exception;

public class ItemCartNotFoundException extends RuntimeException {
    public ItemCartNotFoundException(String message) {
        super(message);
    }
}

