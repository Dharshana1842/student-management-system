package com.studentmanagement.backend.repository;

import com.studentmanagement.backend.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByStudent_Id(Integer studentId);

    boolean existsByStudent_Id(Integer studentId);

    void deleteByStudent_Id(Integer studentId);
}
