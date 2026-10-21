package com.marketplace.userservice.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(Long id) { super("Payment card not found with id: " + id); }
}
