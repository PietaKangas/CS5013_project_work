package com.example.student_management_system.authentication.security;

import org.junit.jupiter.api.Test;

//import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class PasswordPolicyTest {
/*  // do later
    @Test
    void rejectsPasswordShorterThanMinimum() {
        assertThatThrownBy(() -> PasswordPolicy.validate("Short1!", "password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 12 characters");
    }

    @Test
    void rejectsPasswordLongerThanBcryptLimit() {
        String tooLong = "a".repeat(73);

        assertThatThrownBy(() -> PasswordPolicy.validate(tooLong, "password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 72 bytes");
    }

    
    @Test
    void rejectsCommonBreachedPassword() {
        assertThatThrownBy(() -> PasswordPolicy.validate("password1234", "password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("common");
    }
    */
    @Test
    void acceptsStrongPasswordWithinBcryptLimits() {
        assertThatCode(() -> PasswordPolicy.validate("CorrectHorseBatteryStaple!42", "password"))
                .doesNotThrowAnyException();
    }
    
}
