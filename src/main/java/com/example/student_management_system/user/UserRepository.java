package com.example.student_management_system.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Database access for accounts. Spring Data generates the SQL from the method names. */
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AppUser> findByRole(Role role);

    Optional<AppUser> findByIdAndRole(Long id, Role role);
}
