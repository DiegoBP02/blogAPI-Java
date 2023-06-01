package com.example.demo.controllers.exceptions;

public class RateLimitException extends RuntimeException{
    public RateLimitException(){
        super("You have exhausted your API request quota");
    }
}
