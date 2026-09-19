package com.bookngo.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    @Email(message = "Invalid email format")
    @Size(max = 320, message = "Email must not exceed 320 characters")
    private String email;
}
