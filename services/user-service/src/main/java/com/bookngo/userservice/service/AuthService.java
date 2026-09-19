package com.bookngo.userservice.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookngo.userservice.dto.AuthTokenResponse;
import com.bookngo.userservice.dto.LoginRequest;
import com.bookngo.userservice.dto.OtpRequest;
import com.bookngo.userservice.dto.OtpRequestResponse;
import com.bookngo.userservice.dto.OtpVerifyRequest;
import com.bookngo.userservice.dto.UserDto;
import com.bookngo.userservice.entity.OtpVerification;
import com.bookngo.userservice.entity.Role;
import com.bookngo.userservice.entity.User;
import com.bookngo.userservice.exception.ForbiddenException;
import com.bookngo.userservice.exception.UnauthorizedException;
import com.bookngo.userservice.repository.OtpVerificationRepository;
import com.bookngo.userservice.repository.RoleRepository;
import com.bookngo.userservice.repository.UserRepository;
import com.bookngo.userservice.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OtpVerificationRepository otpRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public OtpRequestResponse requestOtp(OtpRequest request) {
        OtpVerification verification = otpService.requestOtp(request.getPhoneNumber());

        return OtpRequestResponse.builder()
                .otpId(verification.getOtpId())
                .phoneNumber(verification.getPhoneNumber())
                .status(verification.getStatus())
                .expiresAt(verification.getExpiresAt())
                .build();
    }

    @Transactional
    public AuthTokenResponse verifyOtp(OtpVerifyRequest request) {
        OtpVerification verification = otpService.verifyOtp(request.getPhoneNumber(), request.getOtpCode());

        // Find or auto-register customer
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber()).orElseGet(() -> {
            log.info("Auto-registering new customer for phone: {}", request.getPhoneNumber());

            Role customerRole = roleRepository.findByRoleCode(Role.CUSTOMER)
                    .orElseGet(() -> roleRepository.save(Role.builder().roleCode(Role.CUSTOMER).build()));

            String phone = request.getPhoneNumber();
            String defaultName = "Customer " + (phone.length() > 4 ? phone.substring(phone.length() - 4) : phone);

            User newUser = User.builder()
                    .phoneNumber(phone)
                    .fullName(defaultName)
                    .accountStatus(User.STATUS_ACTIVE)
                    .passwordHash(null)
                    .roles(new HashSet<>(List.of(customerRole)))
                    .build();

            return userRepository.save(newUser);
        });

        if (!User.STATUS_ACTIVE.equals(user.getAccountStatus())) {
            throw new ForbiddenException("Account is " + user.getAccountStatus());
        }

        // Ensure customer role exists on the user
        boolean hasCustomerRole = user.getRoles().stream()
                .anyMatch(r -> Role.CUSTOMER.equals(r.getRoleCode()));
        if (!hasCustomerRole) {
            roleRepository.findByRoleCode(Role.CUSTOMER).ifPresent(r -> user.getRoles().add(r));
            userRepository.save(user);
        }

        // Link user to OTP verification record
        verification.setUser(user);
        otpRepository.save(verification);

        return buildAuthTokenResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request) {
        String identifier = request.getUsername();

        // Find user by email or phone number
        Optional<User> userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhoneNumber(identifier);
        }

        if (userOpt.isEmpty()) {
            throw new UnauthorizedException("Invalid username or password");
        }

        User user = userOpt.get();

        if (user.getPasswordHash() == null) {
            throw new UnauthorizedException("Invalid username or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        if (!User.STATUS_ACTIVE.equals(user.getAccountStatus())) {
            throw new ForbiddenException("Account is " + user.getAccountStatus());
        }

        // Role authorization check: only THEATRE_OPERATOR and ADMIN can use password login
        boolean isOperatorOrAdmin = user.getRoles().stream()
                .anyMatch(r -> Role.THEATRE_OPERATOR.equals(r.getRoleCode()) || Role.ADMIN.equals(r.getRoleCode()));

        if (!isOperatorOrAdmin) {
            throw new ForbiddenException("Customer accounts must authenticate via OTP");
        }

        return buildAuthTokenResponse(user);
    }

    private AuthTokenResponse buildAuthTokenResponse(User user) {
        List<String> roleCodes = user.getRoles().stream()
                .map(Role::getRoleCode)
                .sorted()
                .collect(Collectors.toList());

        String token = jwtService.generateToken(user.getUserId(), roleCodes, user.getPhoneNumber());

        UserDto userDto = UserDto.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .accountStatus(user.getAccountStatus())
                .roles(roleCodes)
                .createdAt(user.getCreatedAt())
                .build();

        return AuthTokenResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(userDto)
                .build();
    }
}
