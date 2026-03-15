package com.saurabh.user.exception;

public class EmailNotSentException extends RuntimeException {
    public EmailNotSentException() {
        super("Email not sent");
    }

    public EmailNotSentException(String message) {
        super(message);
    }
}
