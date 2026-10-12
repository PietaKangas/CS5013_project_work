package com.example.student_management_system.security;

import com.example.student_management_system.user.AppUser;
import com.example.student_management_system.user.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The logged-in user as Spring Security sees it. Controllers can get it with
 * {@code @AuthenticationPrincipal AuthUser me} and use me.getId() for ownership checks.
 */
public class AuthUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final Role role;

    public AuthUser(AppUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
    }

    public Long getId() { return id; }
    public Role getRole() { return role; }

    // Spring Security expects roles as "ROLE_XXX"; hasRole("LEAD") checks for "ROLE_LEAD".
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    // We don't lock or expire accounts in this project.
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
