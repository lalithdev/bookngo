package com.bookngo.userservice.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookngo.userservice.dto.UserDto;
import com.bookngo.userservice.dto.UserProfileUpdateRequest;
import com.bookngo.userservice.entity.Role;
import com.bookngo.userservice.entity.User;
import com.bookngo.userservice.exception.BadRequestException;
import com.bookngo.userservice.exception.ResourceNotFoundException;
import com.bookngo.userservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return toDto(user);
    }

    @Transactional
    public UserDto updateCurrentUser(UUID userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getEmail() != null) {
            String trimmedEmail = request.getEmail().trim().toLowerCase();
            if (!trimmedEmail.isEmpty()) {
                Optional<User> existingEmailUser = userRepository.findByEmail(trimmedEmail);
                if (existingEmailUser.isPresent() && !existingEmailUser.get().getUserId().equals(userId)) {
                    throw new BadRequestException("Email already in use by another account");
                }
                user.setEmail(trimmedEmail);
            } else {
                user.setEmail(null);
            }
        }

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    private UserDto toDto(User user) {
        List<String> roleCodes = user.getRoles().stream()
                .map(Role::getRoleCode)
                .sorted()
                .collect(Collectors.toList());

        return UserDto.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .accountStatus(user.getAccountStatus())
                .roles(roleCodes)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
