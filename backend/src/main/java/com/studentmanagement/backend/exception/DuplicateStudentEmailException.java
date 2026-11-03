package com.studentmanagement.backend.exception;

public class DuplicateStudentEmailException extends RuntimeException {
    public DuplicateStudentEmailException() {
        super("A student with this email address already exists.");
    }
}
