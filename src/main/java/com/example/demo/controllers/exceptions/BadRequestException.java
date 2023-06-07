package com.example.demo.controllers.exceptions;

public class BadRequestException extends RuntimeException{
    public BadRequestException(String fields){
        super("The following fields are missing: " + fields);
    }
}
