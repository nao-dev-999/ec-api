package com.example.ecapi.exception;

public class EmployeeNotFoundException extends ResourceNotFoundException {

    public EmployeeNotFoundException(Object... args) {
        super(ErrorCode.EMPLOYEE_NOT_FOUND, args);
    }
}
