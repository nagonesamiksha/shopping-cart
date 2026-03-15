package com.saurabh.user.exception;

import com.saurabh.user.entity.ResponseData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class UserAlreadyThereException extends RuntimeException {



    public UserAlreadyThereException() {
        super("user is already there");
    }

    public UserAlreadyThereException(String message) {
        super(message);
    }
}
