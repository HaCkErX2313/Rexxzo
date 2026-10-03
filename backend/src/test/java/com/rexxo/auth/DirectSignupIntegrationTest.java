package com.rexxo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexxo.dto.auth.LoginRequest;
import com.rexxo.dto.auth.RegisterRequest;
import com.rexxo.entity.User;
import com.rexxo.otp.service.EmailOtpService;
import com.rexxo.otp.service.SmsOtpService;
import com.rexxo.repository.CartRepository;
import com.rexxo.repository.UserRepository;
import com.rexxo.repository.WishlistRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DirectSignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailOtpService emailOtpService;

    @MockBean
    private SmsOtpService smsOtpService;

    private static final String TEST_EMAIL = "direct_valid@example.com";
    private static final String TEST_PHONE = "9876543210";
    private static final String TEST_PASSWORD = "StrongSecurePassword123!";

    @BeforeEach
    void setUp() {
        reset(emailOtpService, smsOtpService);
    }

    private void cleanup(String email, String phone) {
        userRepository.findByEmail(email).ifPresent(user -> {
            cartRepository.findByUserId(user.getId()).ifPresent(cartRepository::delete);
            wishlistRepository.findByUserId(user.getId()).ifPresent(wishlistRepository::delete);
            userRepository.delete(user);
        });
        if (phone != null) {
            userRepository.findByPhone(phone).ifPresent(user -> {
                cartRepository.findByUserId(user.getId()).ifPresent(cartRepository::delete);
                wishlistRepository.findByUserId(user.getId()).ifPresent(wishlistRepository::delete);
                userRepository.delete(user);
            });
            String norm = "+91" + phone.replaceAll("[^0-9]", "");
            userRepository.findByPhone(norm).ifPresent(user -> {
                cartRepository.findByUserId(user.getId()).ifPresent(cartRepository::delete);
                wishlistRepository.findByUserId(user.getId()).ifPresent(wishlistRepository::delete);
                userRepository.delete(user);
            });
        }
    }

    @Test
    @Order(1)
    @DisplayName("CASE A: New valid signup -> account created in PostgreSQL & returns JWT")
    void caseA_newValidSignup_createsAccount() throws Exception {
        cleanup(TEST_EMAIL, TEST_PHONE);

        RegisterRequest req = new RegisterRequest(
            "Arjun Sharma",
            "Arjun Sharma",
            TEST_EMAIL,
            TEST_PASSWORD,
            TEST_PHONE
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
            .andExpect(jsonPath("$.user.name").value("Arjun Sharma"));

        // Verify PostgreSQL persistence
        User user = userRepository.findByEmail(TEST_EMAIL).orElse(null);
        assertNotNull(user, "User must be persisted in PostgreSQL");
        assertEquals("Arjun Sharma", user.getName());
        assertEquals("+919876543210", user.getPhone());
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, user.getPassword()), "Password must be BCrypt hashed");
        assertNotEquals(TEST_PASSWORD, user.getPassword(), "Plaintext password must NEVER be stored");
        assertFalse(user.getEmailVerified(), "emailVerified should be false by default without OTP");
        assertFalse(user.getPhoneVerified(), "phoneVerified should be false by default without OTP");

        // Verify cart and wishlist provisioned
        assertTrue(cartRepository.findByUserId(user.getId()).isPresent(), "Cart must be provisioned");
        assertTrue(wishlistRepository.findByUserId(user.getId()).isPresent(), "Wishlist must be provisioned");

        // Verify NO OTP service was invoked
        verifyNoInteractions(emailOtpService);
        verifyNoInteractions(smsOtpService);
    }

    @Test
    @Order(2)
    @DisplayName("CASE B: Duplicate email -> 409 Conflict")
    void caseB_duplicateEmail_returns409() throws Exception {
        RegisterRequest dupEmailReq = new RegisterRequest(
            "Another Name",
            "Another Name",
            TEST_EMAIL, // already registered in Case A
            TEST_PASSWORD,
            "9876543211"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dupEmailReq)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Email is already registered. Please sign in."));
    }

    @Test
    @Order(3)
    @DisplayName("CASE C: Duplicate phone -> 409 Conflict")
    void caseC_duplicatePhone_returns409() throws Exception {
        RegisterRequest dupPhoneReq = new RegisterRequest(
            "Different Name",
            "Different Name",
            "different_unique@example.com",
            TEST_PASSWORD,
            TEST_PHONE // already registered in Case A
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dupPhoneReq)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Mobile number is already registered. Please sign in."));
    }

    @Test
    @Order(4)
    @DisplayName("CASE D: Invalid email -> 400 Bad Request")
    void caseD_invalidEmail_returns400() throws Exception {
        String[] invalidEmails = { "notanemail", "user@", "@example.com", "plainaddress" };
        for (String invalidEmail : invalidEmails) {
            RegisterRequest req = new RegisterRequest(
                "Test User",
                "Test User",
                invalidEmail,
                TEST_PASSWORD,
                "9876543219"
            );

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("valid email")));
        }
    }

    @Test
    @Order(5)
    @DisplayName("CASE E: Invalid phone -> 400 Bad Request")
    void caseE_invalidPhone_returns400() throws Exception {
        String[] invalidPhones = { "12345", "1234567890", "98765", "abcdefghij", "" };
        for (String invalidPhone : invalidPhones) {
            RegisterRequest req = new RegisterRequest(
                "Test User",
                "Test User",
                "valid_new_" + System.currentTimeMillis() + "@example.com",
                TEST_PASSWORD,
                invalidPhone
            );

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsStringIgnoringCase("mobile number")));
        }
    }

    @Test
    @Order(6)
    @DisplayName("CASE F: Weak/invalid password (< 8 chars) -> 400 Bad Request")
    void caseF_weakPassword_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest(
            "Test User",
            "Test User",
            "password_test@example.com",
            "short", // only 5 characters
            "9876543219"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("at least 8 characters")));
    }

    @Test
    @Order(7)
    @DisplayName("CASE G: Correct registered email + password -> login success + JWT")
    void caseG_correctCredentials_loginSuccess() throws Exception {
        LoginRequest req = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value(TEST_EMAIL));
    }

    @Test
    @Order(8)
    @DisplayName("CASE H: Random email + password -> login failure (401)")
    void caseH_randomCredentials_loginFailure() throws Exception {
        LoginRequest req = new LoginRequest("random_nonexistent@example.com", "RandomPassword123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Account not found. Please sign up first."));

        // Also test existing email with wrong password
        LoginRequest wrongPassReq = new LoginRequest(TEST_EMAIL, "WrongPassword123!");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPassReq)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @Order(9)
    @DisplayName("CASE I: Signup with no SMTP configuration -> signup should STILL work")
    void caseI_signupWithoutSmtp_stillWorks() throws Exception {
        // Even if email service throws IllegalStateException (simulating no SMTP config)
        doThrow(new IllegalStateException("SMTP not configured"))
            .when(emailOtpService).sendOtp(anyString(), anyString());

        String email = "no_smtp_user@example.com";
        String phone = "9876543222";
        cleanup(email, phone);

        RegisterRequest req = new RegisterRequest(
            "No SMTP User",
            "No SMTP User",
            email,
            TEST_PASSWORD,
            phone
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value(email));

        // Confirm emailOtpService was never even reached
        verify(emailOtpService, never()).sendOtp(anyString(), anyString());
    }

    @Test
    @Order(10)
    @DisplayName("CASE J: Signup with no SMS provider configuration -> signup should STILL work")
    void caseJ_signupWithoutSms_stillWorks() throws Exception {
        // Even if SMS service throws IllegalStateException (simulating no SMS config)
        doThrow(new IllegalStateException("SMS provider not configured"))
            .when(smsOtpService).sendOtp(anyString(), anyString());

        String email = "no_sms_user@example.com";
        String phone = "9876543233";
        cleanup(email, phone);

        RegisterRequest req = new RegisterRequest(
            "No SMS User",
            "No SMS User",
            email,
            TEST_PASSWORD,
            phone
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value(email));

        // Confirm smsOtpService was never even reached
        verify(smsOtpService, never()).sendOtp(anyString(), anyString());
    }
}
