package com.studentmanagement.backend.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(Integer id) {
        super("Student with ID " + id + " was not found.");
    }
}
