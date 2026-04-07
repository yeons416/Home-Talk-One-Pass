package com.hometalk.onepass.entity.auth;

import com.hometalk.onepass.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    private Household household;

    @Column(name = "login_id", nullable = false, length = 100)
    private String loginId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "nickname", length = 30)
    private String nickname;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.MEMBER;

    // 연관관계
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private LocalAccount localAccount;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    @Builder
    public User(String loginId, String name, String nickname, String email,
                String phoneNumber, UserStatus status, UserRole role) {
        this.loginId = loginId;
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.status = status != null ? status : UserStatus.PENDING;
        this.role = role != null ? role : UserRole.MEMBER;
    }

    // 세대 배정 (관리자 승인 시 호출)
    public void assignHousehold(Household household) {
        this.household = household;
    }

    public void approve() {
        this.status = UserStatus.APPROVED;
    }

    public void reject() {
        this.status = UserStatus.REJECTED;
    }

    // Enum
    public enum UserStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum UserRole {
        ADMIN, RESIDENT, STAFF, MEMBER
    }
}