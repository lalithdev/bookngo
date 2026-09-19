package com.bookngo.userservice.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookngo.userservice.dto.UserDto;
import com.bookngo.userservice.dto.UserProfileUpdateRequest;
import com.bookngo.userservice.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserDto userDto = userService.getCurrentUser(userId);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserDto userDto = userService.updateCurrentUser(userId, request);
        return ResponseEntity.ok(userDto);
    }
}
