package com.studentmanagement.backend.service;

import com.studentmanagement.backend.exception.DuplicateStudentEmailException;
import com.studentmanagement.backend.exception.StudentNotFoundException;
import com.studentmanagement.backend.model.Student;
import com.studentmanagement.backend.repository.StudentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class StudentService {
    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public Student findById(Integer id) {
        return studentRepository.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    public Student create(Student student) {
        normalize(student);
        ensureEmailAvailable(student.getEmail(), null);
        student.setId(null);
        return studentRepository.save(student);
    }

    public Student update(Integer id, Student updates) {
        Student student = findById(id);
        normalize(updates);
        ensureEmailAvailable(updates.getEmail(), id);

        student.setName(updates.getName());
        student.setEmail(updates.getEmail());
        student.setPhone(updates.getPhone());
        student.setDepartment(updates.getDepartment());
        student.setCourse(updates.getCourse());
        student.setYear(updates.getYear());
        student.setDateOfBirth(updates.getDateOfBirth());
        student.setAddress(updates.getAddress());
        student.setAdmissionDate(updates.getAdmissionDate());

        return studentRepository.save(student);
    }

    public void delete(Integer id) {
        Student student = findById(id);
        studentRepository.delete(student);
    }

    private void ensureEmailAvailable(String email, Integer excludedId) {
        studentRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(excludedId))
                .ifPresent(existing -> {
                    throw new DuplicateStudentEmailException();
                });
    }

    private void normalize(Student student) {
        student.setName(student.getName().trim());
        student.setEmail(student.getEmail().trim().toLowerCase(Locale.ROOT));
        student.setPhone(student.getPhone().trim());
        student.setDepartment(student.getDepartment().trim());
        student.setCourse(student.getCourse().trim());
        if (student.getAddress() != null) {
            student.setAddress(student.getAddress().trim());
        }
    }
}
