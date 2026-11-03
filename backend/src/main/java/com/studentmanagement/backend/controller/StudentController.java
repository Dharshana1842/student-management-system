
package com.studentmanagement.backend.controller;

import com.studentmanagement.backend.model.Student;
import com.studentmanagement.backend.service.StudentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.studentmanagement.backend.service.AuthService;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;
    private final AuthService authService;

    public StudentController(StudentService studentService, AuthService authService) {
        this.studentService = studentService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public Student getOwnStudent(Authentication authentication) {
        return authService.getOwnStudent(authentication.getName());
    }

    @PutMapping("/me")
    public Student updateOwnStudent(
            Authentication authentication,
            @Valid @RequestBody OwnProfileUpdate request) {
        return authService.updateOwnStudent(authentication.getName(), request.phone(), request.address());
    }

    @GetMapping
    public List<Student> getAllStudents() {
        return studentService.findAll();
    }

    @GetMapping("/{id}")
    public Student getStudentById(@PathVariable Integer id) {
        return studentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Student addStudent(@Valid @RequestBody Student student) {
        return studentService.create(student);
    }

    @PutMapping("/{id}")
    public Student updateStudent(@PathVariable Integer id, @Valid @RequestBody Student studentDetails) {
        return studentService.update(id, studentDetails);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Integer id) {
        studentService.delete(id);
    }

    public record OwnProfileUpdate(
            @NotBlank @Pattern(regexp = "^\\+?[0-9\\s()\\-]{7,20}$") String phone,
            @Size(max = 500) String address) {
    }
}