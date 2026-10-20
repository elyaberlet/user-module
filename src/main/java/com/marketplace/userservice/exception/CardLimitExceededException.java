package com.marketplace.userservice.exception;

public class CardLimitExceededException extends RuntimeException {
    public CardLimitExceededException(String message) {
        super(message);
    }

    public CardLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
