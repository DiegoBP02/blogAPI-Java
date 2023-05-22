package com.example.demo.services.exceptions;

public class DuplicateKeyException extends DatabaseException{
    public DuplicateKeyException(String msg) {
        super(msg);
    }
}
