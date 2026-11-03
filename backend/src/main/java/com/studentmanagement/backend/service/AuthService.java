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
import java.util.Map;

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
        int passwordBytes = password == null ? 0 : password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 12 || passwordBytes > 72) {
            throw new RegistrationUnavailableException(
                    "Password must contain 12–72 UTF-8 bytes.",
                    Map.of("password", "Password must contain 12–72 UTF-8 bytes."));
        }

        String normalizedEmail = normalizeEmail(email);
        Student rosterStudent = students.findById(studentId)
                .filter(student -> student.getEmail() != null
                        && student.getEmail().trim().equalsIgnoreCase(normalizedEmail))
                .filter(student -> dateOfBirth != null && dateOfBirth.equals(student.getDateOfBirth()))
                .orElseThrow(RegistrationUnavailableException::new);

        if (accounts.existsByStudent_Id(studentId)
                || accounts.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
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
        private final Map<String, String> errors;

        public RegistrationUnavailableException() {
            this(
                    "Registration could not be completed. Check that the student ID, email, and date of birth match the existing roster record, and that the student has not already registered. If the problem continues, contact your school administrator.",
                    Map.of());
        }

        public RegistrationUnavailableException(String message, Map<String, String> errors) {
            super(message);
            this.errors = Map.copyOf(errors);
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }
}
