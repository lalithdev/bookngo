package com.bookngo.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    public static final String CUSTOMER = "CUSTOMER";
    public static final String THEATRE_OPERATOR = "THEATRE_OPERATOR";
    public static final String ADMIN = "ADMIN";

    @Id
    @Column(name = "role_code", length = 32, nullable = false)
    private String roleCode;
}
