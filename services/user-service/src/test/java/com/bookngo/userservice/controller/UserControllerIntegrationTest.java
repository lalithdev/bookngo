package com.bookngo.userservice.controller;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookngo.userservice.dto.UserProfileUpdateRequest;
import com.bookngo.userservice.entity.Role;
import com.bookngo.userservice.entity.User;
import com.bookngo.userservice.repository.OtpVerificationRepository;
import com.bookngo.userservice.repository.RoleRepository;
import com.bookngo.userservice.repository.UserRepository;
import com.bookngo.userservice.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

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
    private JwtService jwtService;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        otpRepository.deleteAll();
        userRepository.deleteAll();

        Role customerRole = roleRepository.findByRoleCode(Role.CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleCode(Role.CUSTOMER).build()));

        testUser = User.builder()
                .fullName("John Doe")
                .phoneNumber("+919876543200")
                .email("john.doe@example.com")
                .accountStatus(User.STATUS_ACTIVE)
                .roles(new HashSet<>(List.of(customerRole)))
                .build();
        testUser = userRepository.save(testUser);

        jwtToken = jwtService.generateToken(testUser.getUserId(), List.of(Role.CUSTOMER), testUser.getPhoneNumber());
    }

    @Test
    void testGetCurrentUser_UnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void testGetCurrentUser_SuccessWithValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getUserId().toString()))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("+919876543200"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
    }

    @Test
    void testUpdateCurrentUser_Success() throws Exception {
        UserProfileUpdateRequest updateRequest = UserProfileUpdateRequest.builder()
                .fullName("Johnathan Doe")
                .email("johnathan.updated@example.com")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Johnathan Doe"))
                .andExpect(jsonPath("$.email").value("johnathan.updated@example.com"));
    }

    @Test
    void testUpdateCurrentUser_InvalidEmailFormat() throws Exception {
        UserProfileUpdateRequest updateRequest = UserProfileUpdateRequest.builder()
                .email("invalid-email-format")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("email"));
    }
}
