package com.studentmanagement.backend.service;

import com.studentmanagement.backend.model.AccountRole;
import com.studentmanagement.backend.model.Student;
import com.studentmanagement.backend.model.UserAccount;
import com.studentmanagement.backend.repository.StudentRepository;
import com.studentmanagement.backend.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;

@Service
public class AuthService {
    private final StudentRepository students;
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            StudentRepository students,
            UserAccountRepository accounts,
            PasswordEncoder passwordEncoder) {
        this.students = students;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerStudent(Integer studentId, String email, LocalDate dateOfBirth, String password) {
        String normalizedEmail = normalizeEmail(email);
        Student rosterStudent = students.findById(studentId)
                .filter(student -> student.getEmail().equalsIgnoreCase(normalizedEmail))
                .filter(student -> dateOfBirth != null && dateOfBirth.equals(student.getDateOfBirth()))
                .orElseThrow(RegistrationUnavailableException::new);

        if (accounts.existsByStudent_Id(studentId)
                || accounts.findByEmailIgnoreCase(normalizedEmail).isPresent()
                || password == null
                || password.getBytes(StandardCharsets.UTF_8).length < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegistrationUnavailableException();
        }

        accounts.save(new UserAccount(
                normalizedEmail,
                passwordEncoder.encode(password),
                AccountRole.STUDENT,
                rosterStudent));
    }

    @Transactional(readOnly = true)
    public SessionView getSession(String email) {
        UserAccount account = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(RegistrationUnavailableException::new);
        return new SessionView(account.getEmail(), account.getRole().name(),
                account.getStudent() == null ? null : account.getStudent().getId());
    }

    @Transactional(readOnly = true)
    public Student getOwnStudent(String email) {
        UserAccount account = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(RegistrationUnavailableException::new);
        if (account.getRole() != AccountRole.STUDENT || account.getStudent() == null) {
            throw new RegistrationUnavailableException();
        }
        return students.findById(account.getStudent().getId())
                .orElseThrow(RegistrationUnavailableException::new);
    }

    @Transactional
    public Student updateOwnStudent(String email, String phone, String address) {
        UserAccount account = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(RegistrationUnavailableException::new);
        if (account.getRole() != AccountRole.STUDENT || account.getStudent() == null) {
            throw new RegistrationUnavailableException();
        }
        Student student = students.findById(account.getStudent().getId())
                .orElseThrow(RegistrationUnavailableException::new);
        student.setPhone(phone.trim());
        student.setAddress(address == null ? null : address.trim());
        return students.save(student);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record SessionView(String email, String role, Integer studentId) {
    }

    public static class RegistrationUnavailableException extends RuntimeException {
        public RegistrationUnavailableException() {
            super("Registration is unavailable for the supplied roster details.");
        }
    }
}
