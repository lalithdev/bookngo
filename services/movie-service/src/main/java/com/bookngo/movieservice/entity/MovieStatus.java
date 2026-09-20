package com.bookngo.movieservice.entity;

public enum MovieStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    ARCHIVED;

    public static final String DEFAULT_STATUS = "ACTIVE";
}
