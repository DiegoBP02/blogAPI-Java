package com.example.demo.services.exceptions;

public class InvalidOldPasswordException extends RuntimeException {
    public InvalidOldPasswordException(String msg) {
        super(msg);
    }
}
