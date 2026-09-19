package com.bookngo.userservice.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "otp_verifications",
    indexes = {
        @Index(name = "idx_otp_verifications_phone_expiry", columnList = "phone_number, expires_at DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_LOCKED = "LOCKED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "otp_id", nullable = false, updatable = false)
    private UUID otpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "phone_number", length = 32, nullable = false)
    private String phoneNumber;

    @Column(name = "otp_secret_hash", length = 255, nullable = false)
    private String otpSecretHash;

    @Column(name = "status", length = 32, nullable = false)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "rate_limit_metadata", nullable = false, columnDefinition = "text")
    @Builder.Default
    private String rateLimitMetadata = "{}";

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (status == null) {
            status = STATUS_PENDING;
        }
        if (attemptCount == null) {
            attemptCount = 0;
        }
        if (rateLimitMetadata == null) {
            rateLimitMetadata = "{}";
        }
    }
}
