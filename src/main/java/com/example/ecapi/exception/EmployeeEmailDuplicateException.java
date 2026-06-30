package com.example.ecapi.exception;

public class EmployeeEmailDuplicateException extends ConflictException {

    public EmployeeEmailDuplicateException(Object... args) {
        super(ErrorCode.EMPLOYEE_EMAIL_DUPLICATE, args);
    }
}
