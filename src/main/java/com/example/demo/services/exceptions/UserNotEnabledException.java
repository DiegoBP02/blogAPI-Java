package com.example.demo.services.exceptions;

public class UserNotEnabledException extends RuntimeException {
    public UserNotEnabledException() {
        super("User is not enabled, please verify your email first");
    }
}
