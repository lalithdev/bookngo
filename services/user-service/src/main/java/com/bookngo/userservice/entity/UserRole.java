package com.bookngo.userservice.entity;

/**
 * Represents the join relationship mapped in table user_roles.
 * The association is mapped via {@code @ManyToMany} in {@link User#getRoles()}.
 */
public final class UserRole {
    public static final String TABLE_NAME = "user_roles";

    private UserRole() {
    }
}
