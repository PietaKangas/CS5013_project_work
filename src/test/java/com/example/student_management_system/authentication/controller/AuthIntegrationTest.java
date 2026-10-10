package com.example.student_management_system.authentication.controller;

import com.example.student_management_system.authentication.model.Role;
import com.example.student_management_system.authentication.model.User;
import com.example.student_management_system.authentication.repository.UserRepository;
import com.example.student_management_system.authentication.security.JwtService;
import com.example.student_management_system.authentication.security.LoginRateLimiter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


// check tests
@SpringBootTest
@ActiveProfiles("test")
@Import(AuthIntegrationTest.ShortRateLimitConfig.class)
@Transactional
@Sql(
    scripts = "/sql/one_lead_index.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class AuthIntegrationTest {

    @TestConfiguration
    static class ShortRateLimitConfig {
        @Bean
        @Primary
        LoginRateLimiter loginRateLimiter() {
            return new LoginRateLimiter(2, Duration.ofMillis(300));
        }
    }

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        userRepository.deleteAll();
        loginRateLimiter.reset();
    }

    @Test
    void registerWithRoleLeadShouldCreateStudent() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"password\":\"StrongPass123\",\"role\":\"LEAD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User savedUser = userRepository.findByEmail("student@example.com").orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void loginUnknownEmailAndWrongPasswordReturnSameStatusAndBody() throws Exception {
        String unknownEmailBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.com\",\"password\":\"WrongPass123!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        
        loginRateLimiter.reset();
        userRepository.save(new User("existing@example.com", "encoded", Role.STUDENT));

        String wrongPasswordBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"existing@example.com\",\"password\":\"WrongPass123!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknownEmailBody).isEqualTo(wrongPasswordBody);
    }


@Test
void loginWithCorrectCredentialsReturns200AndToken() throws Exception {
    String rawPassword = "StrongPass123!";
    String encodedPassword =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                    .encode(rawPassword);

    userRepository.save(
            new User("login@example.com", encodedPassword, Role.STUDENT)
    );

    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "login@example.com",
                                "password": "StrongPass123!"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
}
    

@Test
void loginRateLimitReturns429AndAllowsRetryAfterBackoff() throws Exception {
    String rawPassword = "StrongPass123!";
    String encodedPassword =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                    .encode(rawPassword);

    userRepository.save(
            new User("rateuser@example.com", encodedPassword, Role.STUDENT)
    );

    // First failed attempt
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"rateuser@example.com","password":"WrongPass123!"}
                            """))
            .andExpect(status().isUnauthorized());

    // Second failed attempt
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"rateuser@example.com","password":"WrongPass123!"}
                            """))
            .andExpect(status().isUnauthorized());

    // Third attempt must be rate-limited
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"rateuser@example.com","password":"WrongPass123!"}
                            """))
            .andExpect(status().isTooManyRequests());

    Thread.sleep(400);

    // Login should succeed without manually resetting the limiter
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"rateuser@example.com","password":"StrongPass123!"}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
}

    @Test
    void invalidJwtVariantsAreRejectedWith401() throws Exception {
        String secret = "test-secret-for-jwt-32-bytes-long-abc";
        String expiredToken = Jwts.builder()
                .subject("student@example.com")
                .issuedAt(new Date(System.currentTimeMillis() - 60000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        String tamperedToken = jwtService.generateToken(org.springframework.security.core.userdetails.User
                .withUsername("student@example.com")
                .password("pw")
                .roles("STUDENT")
                .build()) + "x";

        String algNoneToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJzdHVkZW50QGV4YW1wbGUuY29tIn0.";

        mockMvc.perform(authorizedTransferRequest(expiredToken, "{\"newLeadUserId\":1}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(authorizedTransferRequest(tamperedToken, "{\"newLeadUserId\":1}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(authorizedTransferRequest(algNoneToken, "{\"newLeadUserId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noTokenOnTransferReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/lead/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLeadUserId\": 2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCallingTransferReturns403() throws Exception {
        User student = userRepository.save(new User("student@example.com", "encoded", Role.STUDENT));
        String token = tokenFor(student);

        mockMvc.perform(post("/api/admin/lead/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLeadUserId\": 2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferToCurrentLeadReturns400() throws Exception {
        User lead = userRepository.save(new User("lead@example.com", "encoded", Role.LEAD));
        String token = tokenFor(lead);

        mockMvc.perform(post("/api/admin/lead/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLeadUserId\":" + lead.getId() + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void successfulTransferLeavesExactlyOneLeadAndOldLeadBecomesStudent() throws Exception {
        User oldLead = userRepository.save(new User("oldlead@example.com", "encoded", Role.LEAD));
        User target = userRepository.save(new User("target@example.com", "encoded", Role.STUDENT));

        String token = tokenFor(oldLead);

        mockMvc.perform(post("/api/admin/lead/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLeadUserId\":" + target.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLeadEmail").value("oldlead@example.com"))
                .andExpect(jsonPath("$.newLeadEmail").value("target@example.com"));

        assertThat(userRepository.countByRole(Role.LEAD)).isEqualTo(1L);
        assertThat(userRepository.findById(oldLead.getId()).orElseThrow().getRole()).isEqualTo(Role.STUDENT);
        assertThat(userRepository.findById(target.getId()).orElseThrow().getRole()).isEqualTo(Role.LEAD);
    }

    @Test
    void oldLeadTokenGets403AfterDemotion() throws Exception {
        User oldLead = userRepository.save(new User("oldlead@example.com", "encoded", Role.LEAD));
        User target = userRepository.save(new User("target@example.com", "encoded", Role.STUDENT));
        String oldLeadToken = tokenFor(oldLead);

        mockMvc.perform(post("/api/admin/lead/transfer")
                        .header("Authorization", "Bearer " + oldLeadToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLeadUserId\":" + target.getId() + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/lead/transfer")
                        .header("Authorization", "Bearer " + oldLeadToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLeadUserId\":" + target.getId() + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void savingSecondLeadDirectlyViaRepositoryThrowsDataIntegrityViolationException() {
        userRepository.saveAndFlush(new User("lead1@example.com", "encoded", Role.LEAD));

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(new User("lead2@example.com", "encoded", Role.LEAD));
        });
    }

    private MockHttpServletRequestBuilder authorizedTransferRequest(String token, String bodyJson) {
        return post("/api/admin/lead/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyJson);
    }

    private String tokenFor(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
        return jwtService.generateToken(userDetails);
    }
}
