package com.flagship.claimcheck.api;

public class IdempotencyKeyConflictException extends RuntimeException {
    public IdempotencyKeyConflictException() {
        super("Idempotency-Key has already been used with a different claim");
    }
}
