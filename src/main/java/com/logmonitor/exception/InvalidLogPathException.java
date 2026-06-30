package com.logmonitor.exception;

public class InvalidLogPathException extends RuntimeException {

    public InvalidLogPathException(String message) {
        super(message);
    }
}
