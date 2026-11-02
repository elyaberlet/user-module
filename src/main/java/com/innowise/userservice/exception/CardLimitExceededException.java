package com.innowise.userservice.exception;

public class CardLimitExceededException extends RuntimeException {
    public CardLimitExceededException(Long userId) {
        super("User " + userId + " cannot have more than 5 active payment cards");
    }
}
