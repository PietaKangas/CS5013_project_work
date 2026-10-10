package com.example.student_management_system.authentication.repository;

import com.example.student_management_system.authentication.model.Role;
import com.example.student_management_system.authentication.model.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findWithLockByEmail(String email);

    boolean existsByEmail(String email);
    
    long countByRole(Role role);
}