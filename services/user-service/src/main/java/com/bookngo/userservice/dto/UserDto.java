package com.bookngo.userservice.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID userId;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String accountStatus;
    private List<String> roles;
    private OffsetDateTime createdAt;
}
