package com.example.ecapi.exception;

public class OptimisticLockConflictException extends ConflictException {

    public OptimisticLockConflictException() {
        super(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
    }
}
