package com.bookngo.userservice.controller;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookngo.userservice.dto.LoginRequest;
import com.bookngo.userservice.dto.OtpRequest;
import com.bookngo.userservice.dto.OtpVerifyRequest;
import com.bookngo.userservice.entity.Role;
import com.bookngo.userservice.entity.User;
import com.bookngo.userservice.repository.OtpVerificationRepository;
import com.bookngo.userservice.repository.RoleRepository;
import com.bookngo.userservice.repository.UserRepository;
import com.bookngo.userservice.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        otpRepository.deleteAll();
        userRepository.deleteAll();

        if (roleRepository.findByRoleCode(Role.CUSTOMER).isEmpty()) {
            roleRepository.save(Role.builder().roleCode(Role.CUSTOMER).build());
        }
        if (roleRepository.findByRoleCode(Role.THEATRE_OPERATOR).isEmpty()) {
            roleRepository.save(Role.builder().roleCode(Role.THEATRE_OPERATOR).build());
        }
        if (roleRepository.findByRoleCode(Role.ADMIN).isEmpty()) {
            roleRepository.save(Role.builder().roleCode(Role.ADMIN).build());
        }
    }

    @Test
    void testRequestOtp_ValidPhone() throws Exception {
        OtpRequest request = OtpRequest.builder()
                .phoneNumber("+919876543210")
                .build();

        mockMvc.perform(post("/api/v1/auth/otp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpId").isNotEmpty())
                .andExpect(jsonPath("$.phoneNumber").value("+919876543210"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void testRequestOtp_InvalidPhonePattern() throws Exception {
        OtpRequest request = OtpRequest.builder()
                .phoneNumber("invalid-phone")
                .build();

        mockMvc.perform(post("/api/v1/auth/otp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("phoneNumber"));
    }

    @Test
    void testVerifyOtp_NewCustomerAutoRegistration() throws Exception {
        String phone = "+919876543211";
        otpService.requestOtp(phone);
        String generatedOtp = otpService.getLastGeneratedOtp();

        OtpVerifyRequest verifyRequest = OtpVerifyRequest.builder()
                .phoneNumber(phone)
                .otpCode(generatedOtp)
                .build();

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.phoneNumber").value(phone))
                .andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.accountStatus").value("ACTIVE"));
    }

    @Test
    void testVerifyOtp_InvalidOtp() throws Exception {
        String phone = "+919876543212";
        otpService.requestOtp(phone);

        OtpVerifyRequest verifyRequest = OtpVerifyRequest.builder()
                .phoneNumber(phone)
                .otpCode("000000")
                .build();

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"));
    }

    @Test
    void testLogin_OperatorSuccess() throws Exception {
        Role operatorRole = roleRepository.findByRoleCode(Role.THEATRE_OPERATOR).orElseThrow();
        User operator = User.builder()
                .fullName("Theatre Operator 1")
                .phoneNumber("+919876543220")
                .email("operator@cinema.com")
                .passwordHash(passwordEncoder.encode("OperatorSecret123"))
                .accountStatus(User.STATUS_ACTIVE)
                .roles(new HashSet<>(List.of(operatorRole)))
                .build();
        userRepository.save(operator);

        LoginRequest loginRequest = LoginRequest.builder()
                .username("operator@cinema.com")
                .password("OperatorSecret123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("operator@cinema.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("THEATRE_OPERATOR"));
    }

    @Test
    void testLogin_InvalidPassword() throws Exception {
        Role operatorRole = roleRepository.findByRoleCode(Role.THEATRE_OPERATOR).orElseThrow();
        User operator = User.builder()
                .fullName("Theatre Operator 1")
                .phoneNumber("+919876543221")
                .email("operator2@cinema.com")
                .passwordHash(passwordEncoder.encode("OperatorSecret123"))
                .accountStatus(User.STATUS_ACTIVE)
                .roles(new HashSet<>(List.of(operatorRole)))
                .build();
        userRepository.save(operator);

        LoginRequest loginRequest = LoginRequest.builder()
                .username("operator2@cinema.com")
                .password("WrongPassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void testLogin_CustomerForbidden() throws Exception {
        Role customerRole = roleRepository.findByRoleCode(Role.CUSTOMER).orElseThrow();
        User customer = User.builder()
                .fullName("Customer User")
                .phoneNumber("+919876543222")
                .email("customer@mail.com")
                .passwordHash(passwordEncoder.encode("CustomerPassword"))
                .accountStatus(User.STATUS_ACTIVE)
                .roles(new HashSet<>(List.of(customerRole)))
                .build();
        userRepository.save(customer);

        LoginRequest loginRequest = LoginRequest.builder()
                .username("customer@mail.com")
                .password("CustomerPassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
