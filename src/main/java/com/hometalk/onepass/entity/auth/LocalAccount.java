package com.hometalk.onepass.entity.auth;

import com.hometalk.onepass.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "local_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalAccount extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    // user_id가 PK이자 FK이므로 @MapsId 사용
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "login_email", nullable = false, unique = true, length = 100)
    private String loginEmail;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "email_verify_token", length = 255)
    private String emailVerifyToken;

    @Builder
    public LocalAccount(User user, String loginEmail, String passwordHash,
                        String emailVerifyToken) {
        this.user = user;
        this.loginEmail = loginEmail;
        this.passwordHash = passwordHash;
        this.emailVerified = false;
        this.emailVerifyToken = emailVerifyToken;
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifyToken = null;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}