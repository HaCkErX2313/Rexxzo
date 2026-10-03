package com.rexxo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexxo.dto.auth.LoginRequest;
import com.rexxo.dto.auth.ResendOtpRequest;
import com.rexxo.dto.auth.SignupInitiateRequest;
import com.rexxo.dto.auth.SignupVerifyRequest;
import com.rexxo.entity.OtpVerification;
import com.rexxo.entity.PendingRegistration;
import com.rexxo.entity.User;
import com.rexxo.otp.service.EmailOtpService;
import com.rexxo.otp.service.SmsOtpService;
import com.rexxo.repository.OtpVerificationRepository;
import com.rexxo.repository.PendingRegistrationRepository;
import com.rexxo.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SignupOtpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailOtpService emailOtpService;

    @MockBean
    private SmsOtpService smsOtpService;

    private static final String TEST_PASSWORD = "StrongSecurePassword123!";

    @BeforeEach
    void setUp() {
        reset(emailOtpService, smsOtpService);
    }

    private void cleanup(String email, String phone) {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
        userRepository.findByPhone(phone).ifPresent(userRepository::delete);
        pendingRegistrationRepository.findTopByEmailOrderByCreatedAtDesc(email).ifPresent(pendingRegistrationRepository::delete);
    }

    @Test
    @Order(1)
    @DisplayName("TEST 1: Random email + random phone initiates OTP flow but DOES NOT create account yet")
    void test1_initiateSignup_doesNotCreateAccount() throws Exception {
        String email = "securitytest1@rexxo.com";
        String phone = "+919988776601";
        cleanup(email, phone);

        SignupInitiateRequest request = new SignupInitiateRequest(
            "Security Tester",
            email,
            phone,
            TEST_PASSWORD,
            TEST_PASSWORD
        );

        MvcResult result = mockMvc.perform(post("/api/auth/signup/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.verificationId").exists())
            .andExpect(jsonPath("$.maskedEmail").exists())
            .andExpect(jsonPath("$.maskedPhone").exists())
            .andExpect(jsonPath("$.otp").doesNotExist())
            .andExpect(jsonPath("$.emailOtp").doesNotExist())
            .andExpect(jsonPath("$.phoneOtp").doesNotExist())
            .andReturn();

        // 1. Verify User does NOT exist in PostgreSQL
        assertFalse(userRepository.existsByEmail(email), "Permanent user MUST NOT exist in PostgreSQL yet!");

        // 2. Verify real OTP services were called
        verify(emailOtpService, times(1)).sendOtp(eq(email), anyString());
        verify(smsOtpService, times(1)).sendOtp(eq(phone), anyString());

        // 3. Verify OTP in DB is HASHED, never stored in plaintext
        PendingRegistration pending = pendingRegistrationRepository.findTopByEmailOrderByCreatedAtDesc(email).orElseThrow();
        OtpVerification emailOtpRec = otpVerificationRepository.findTopBySessionIdAndIdentifierTypeAndPurposeOrderByCreatedAtDesc(
            pending.getVerificationId(), OtpVerification.IdentifierType.EMAIL, OtpVerification.Purpose.SIGNUP
        ).orElseThrow();

        assertNotEquals("123456", emailOtpRec.getOtpHash());
        assertTrue(emailOtpRec.getOtpHash().startsWith("$2a$") || emailOtpRec.getOtpHash().startsWith("$2b$"),
            "OTP hash MUST be a valid BCrypt hash!");
    }

    @Test
    @Order(2)
    @DisplayName("TEST 2: Correct email OTP + wrong phone OTP -> signup MUST FAIL")
    void test2_correctEmail_wrongPhone_fails() throws Exception {
        String email = "test2@rexxo.com";
        String phone = "+919988776602";
        cleanup(email, phone);

        ArgumentCaptor<String> emailOtpCaptor = ArgumentCaptor.forClass(String.class);

        SignupInitiateRequest req = new SignupInitiateRequest(
            "Tester Two", email, phone, "Password123!", "Password123!"
        );
        MvcResult initResult = mockMvc.perform(post("/api/auth/signup/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        verify(emailOtpService).sendOtp(eq(email), emailOtpCaptor.capture());
        String realEmailOtp = emailOtpCaptor.getValue();
        String verificationId = objectMapper.readTree(initResult.getResponse().getContentAsString()).get("verificationId").asText();

        // Submit correct email OTP + WRONG phone OTP
        SignupVerifyRequest verifyReq = new SignupVerifyRequest(verificationId, realEmailOtp, "000000");
        mockMvc.perform(post("/api/auth/signup/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));

        assertFalse(userRepository.existsByEmail(email), "User account MUST NOT be created on partial verification!");
    }

    @Test
    @Order(3)
    @DisplayName("TEST 3: Wrong email OTP + correct phone OTP -> signup MUST FAIL")
    void test3_wrongEmail_correctPhone_fails() throws Exception {
        String email = "test3@rexxo.com";
        String phone = "+919988776603";
        cleanup(email, phone);

        ArgumentCaptor<String> phoneOtpCaptor = ArgumentCaptor.forClass(String.class);

        SignupInitiateRequest req = new SignupInitiateRequest(
            "Tester Three", email, phone, "Password123!", "Password123!"
        );
        MvcResult initResult = mockMvc.perform(post("/api/auth/signup/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        verify(smsOtpService).sendOtp(eq(phone), phoneOtpCaptor.capture());
        String realPhoneOtp = phoneOtpCaptor.getValue();
        String verificationId = objectMapper.readTree(initResult.getResponse().getContentAsString()).get("verificationId").asText();

        // Submit WRONG email OTP + correct phone OTP
        SignupVerifyRequest verifyReq = new SignupVerifyRequest(verificationId, "000000", realPhoneOtp);
        mockMvc.perform(post("/api/auth/signup/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));

        assertFalse(userRepository.existsByEmail(email), "User account MUST NOT be created on partial verification!");
    }

    @Test
    @Order(4)
    @DisplayName("TEST 4: Expired email OTP -> MUST FAIL")
    void test4_expiredEmailOtp_fails() throws Exception {
        String email = "test4@rexxo.com";
        String phone = "+919988776604";
        cleanup(email, phone);

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);

        SignupInitiateRequest req = new SignupInitiateRequest(
            "Tester Four", email, phone, "Password123!", "Password123!"
        );
        MvcResult initResult = mockMvc.perform(post("/api/auth/signup/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        verify(emailOtpService).sendOtp(eq(email), emailCaptor.capture());
        verify(smsOtpService).sendOtp(eq(phone), phoneCaptor.capture());
        String verificationId = objectMapper.readTree(initResult.getResponse().getContentAsString()).get("verificationId").asText();

        // Expire the email OTP in database
        OtpVerification emailRec = otpVerificationRepository.findTopBySessionIdAndIdentifierTypeAndPurposeOrderByCreatedAtDesc(
            verificationId, OtpVerification.IdentifierType.EMAIL, OtpVerification.Purpose.SIGNUP
        ).orElseThrow();
        emailRec.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otpVerificationRepository.save(emailRec);

        SignupVerifyRequest verifyReq = new SignupVerifyRequest(verificationId, emailCaptor.getValue(), phoneCaptor.getValue());
        mockMvc.perform(post("/api/auth/signup/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expired")));
    }

    @Test
    @Order(5)
    @DisplayName("TEST 5: Expired phone OTP -> MUST FAIL")
    void test5_expiredPhoneOtp_fails() throws Exception {
        String email = "test5@rexxo.com";
        String phone = "+919988776605";
        cleanup(email, phone);

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);

        SignupInitiateRequest req = new SignupInitiateRequest(
            "Tester Five", email, phone, "Password123!", "Password123!"
        );
        MvcResult initResult = mockMvc.perform(post("/api/auth/signup/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        verify(emailOtpService).sendOtp(eq(email), emailCaptor.capture());
        verify(smsOtpService).sendOtp(eq(phone), phoneCaptor.capture());
        String verificationId = objectMapper.readTree(initResult.getResponse().getContentAsString()).get("verificationId").asText();

        // Expire the phone OTP in database
        OtpVerification phoneRec = otpVerificationRepository.findTopBySessionIdAndIdentifierTypeAndPurposeOrderByCreatedAtDesc(
            verificationId, OtpVerification.IdentifierType.PHONE, OtpVerification.Purpose.SIGNUP
        ).orElseThrow();
        phoneRec.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otpVerificationRepository.save(phoneRec);

        SignupVerifyRequest verifyReq = new SignupVerifyRequest(verificationId, emailCaptor.getValue(), phoneCaptor.getValue());
        mockMvc.perform(post("/api/auth/signup/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expired")));
    }

    @Test
    @Order(6)
    @DisplayName("TEST 6: Correct email OTP + correct phone OTP -> account MUST be created")
    void test6_bothCorrect_accountCreatedSuccessfully() throws Exception {
        String email = "verified@rexxo.com";
        String phone = "+919988776606";
        cleanup(email, phone);

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);

        SignupInitiateRequest req = new SignupInitiateRequest(
            "Verified User", email, phone, TEST_PASSWORD, TEST_PASSWORD
        );
        MvcResult initResult = mockMvc.perform(post("/api/auth/signup/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        verify(emailOtpService).sendOtp(eq(email), emailCaptor.capture());
        verify(smsOtpService).sendOtp(eq(phone), phoneCaptor.capture());
        String verificationId = objectMapper.readTree(initResult.getResponse().getContentAsString()).get("verificationId").asText();

        // Submit BOTH correct OTPs
        SignupVerifyRequest verifyReq = new SignupVerifyRequest(verificationId, emailCaptor.getValue(), phoneCaptor.getValue());
        mockMvc.perform(post("/api/auth/signup/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.email").value(email))
            .andExpect(jsonPath("$.user.emailVerified").value(true))
            .andExpect(jsonPath("$.user.phoneVerified").value(true));

        // Verify permanent User exists in database
        User createdUser = userRepository.findByEmail(email).orElseThrow();
        assertEquals("Verified User", createdUser.getName());
        assertEquals(phone, createdUser.getPhone());
        assertTrue(createdUser.getEmailVerified());
        assertTrue(createdUser.getPhoneVerified());
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, createdUser.getPassword()), "Password MUST be hashed with BCrypt!");
    }

    @Test
    @Order(7)
    @DisplayName("TEST 7: Try registering the same email again -> MUST FAIL (409)")
    void test7_duplicateEmail_fails() throws Exception {
        SignupInitiateRequest req = new SignupInitiateRequest(
            "Duplicate User", "verified@rexxo.com", "+919988776699", TEST_PASSWORD, TEST_PASSWORD
        );
        mockMvc.perform(post("/api/auth/signup/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already registered")));
    }

    @Test
    @Order(8)
    @DisplayName("TEST 8: Try registering the same phone again -> MUST FAIL (409)")
    void test8_duplicatePhone_fails() throws Exception {
        SignupInitiateRequest req = new SignupInitiateRequest(
            "Duplicate Phone User", "diffemail@rexxo.com", "+919988776606", TEST_PASSWORD, TEST_PASSWORD
        );
        mockMvc.perform(post("/api/auth/signup/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already registered")));
    }

    @Test
    @Order(9)
    @DisplayName("TEST 9: Enter wrong OTP 5 times -> OTP MUST become invalid")
    void test9_fiveIncorrectAttempts_invalidatesOtp() throws Exception {
        String email = "attempt@rexxo.com";
        String phone = "+919988776609";
        cleanup(email, phone);

        SignupInitiateRequest req = new SignupInitiateRequest(
            "Attempt Tester", email, phone, TEST_PASSWORD, TEST_PASSWORD
        );
        MvcResult initResult = mockMvc.perform(post("/api/auth/signup/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        String verificationId = objectMapper.readTree(initResult.getResponse().getContentAsString()).get("verificationId").asText();

        // 4 initial wrong attempts (return 401 Unauthorized)
        for (int i = 1; i <= 4; i++) {
            SignupVerifyRequest verifyReq = new SignupVerifyRequest(verificationId, "999999", "999999");
            mockMvc.perform(post("/api/auth/signup/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isUnauthorized());
        }

        // 5th attempt: reached max attempts -> returns 400 Bad Request ("Too many incorrect attempts")
        SignupVerifyRequest verifyReq5 = new SignupVerifyRequest(verificationId, "999999", "999999");
        mockMvc.perform(post("/api/auth/signup/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq5)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Too many incorrect attempts")));
    }

    @Test
    @Order(10)
    @DisplayName("TEST 10: Spam resend OTP requests -> rate limiting MUST activate (429)")
    void test10_spamResend_rateLimitingActivates() throws Exception {
        String email = "spam@rexxo.com";
        String phone = "+919988776610";
        cleanup(email, phone);

        SignupInitiateRequest req = new SignupInitiateRequest(
            "Spam Tester", email, phone, TEST_PASSWORD, TEST_PASSWORD
        );
        MvcResult initResult = mockMvc.perform(post("/api/auth/signup/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        String verificationId = objectMapper.readTree(initResult.getResponse().getContentAsString()).get("verificationId").asText();

        // Immediate resend request within 60-second cooldown
        ResendOtpRequest resendReq = new ResendOtpRequest(verificationId);
        mockMvc.perform(post("/api/auth/signup/resend-email-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resendReq)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Please wait")));
    }

    @Test
    @Order(11)
    @DisplayName("TEST 11: Login with direct registration (emailVerified=false) -> MUST succeed")
    void test11_loginWithDirectSignup_succeeds() throws Exception {
        String email = "unverified@rexxo.com";
        String phone = "+919988776611";
        cleanup(email, phone);

        User unverifiedUser = User.builder()
            .name("Unverified")
            .email(email)
            .password(passwordEncoder.encode("Password123!"))
            .phone(phone)
            .emailVerified(false)
            .phoneVerified(false)
            .isActive(true)
            .build();
        userRepository.save(unverifiedUser);

        LoginRequest loginReq = new LoginRequest(email, "Password123!");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    @Order(12)
    @DisplayName("TEST 12: Login after successful verification -> MUST succeed with correct password")
    void test12_loginAfterVerification_succeeds() throws Exception {
        LoginRequest loginReq = new LoginRequest("verified@rexxo.com", TEST_PASSWORD);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.email").value("verified@rexxo.com"));
    }

    @Test
    @Order(13)
    @DisplayName("TEST 13: Wrong password -> MUST fail")
    void test13_wrongPassword_fails() throws Exception {
        LoginRequest loginReq = new LoginRequest("verified@rexxo.com", "WrongPassword123!");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid email or password")));
    }

    @Test
    @Order(14)
    @DisplayName("TEST 14: Nonexistent email login -> MUST fail")
    void test14_nonexistentEmail_fails() throws Exception {
        LoginRequest loginReq = new LoginRequest("ghostuser9999@rexxo.com", "Password123!");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Account not found")));
    }
}
