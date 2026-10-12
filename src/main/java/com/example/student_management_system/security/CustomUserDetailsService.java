package com.example.student_management_system.security;

import com.example.student_management_system.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Tells Spring Security how to load an account by email (our "username"). */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalized)
                .map(AuthUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + normalized));
    }
}
