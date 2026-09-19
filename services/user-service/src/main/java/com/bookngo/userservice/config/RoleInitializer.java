package com.bookngo.userservice.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bookngo.userservice.entity.Role;
import com.bookngo.userservice.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        List<String> defaultRoles = List.of(Role.CUSTOMER, Role.THEATRE_OPERATOR, Role.ADMIN);
        for (String roleCode : defaultRoles) {
            if (roleRepository.findByRoleCode(roleCode).isEmpty()) {
                roleRepository.save(Role.builder().roleCode(roleCode).build());
                log.info("Seeded role: {}", roleCode);
            }
        }
    }
}
