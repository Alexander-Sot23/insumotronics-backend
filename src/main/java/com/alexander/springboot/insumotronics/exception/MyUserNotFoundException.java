package com.alexander.springboot.insumotronics.exception;

public class MyUserNotFoundException extends RuntimeException {
    public MyUserNotFoundException(String message) {
        super(message);
    }
}

