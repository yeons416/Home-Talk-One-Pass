package com.hometalk.onepass.entity.dashboard;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Builder;
import org.springframework.data.annotation.Id;

public class BillingAdmin {

    @Id // 1. 반드시 PK를 지정해야 합니다.
    // 만약 DB에서 자동 생성되는 값이 아니라 직접 입력하는 값이라면 아래 줄은 생략하세요.
    private Long userId;

    @Enumerated(EnumType.STRING) // 2. Enum 타입인 경우 DB 저장을 위해 권장됩니다.
    private STATUS status;

    private String name;
    private String nickname;
    private String dong;
    private String ho;

    @Builder
    public BillingAdmin(Long userId, STATUS status, String name, String nickname, String dong, String ho) {
        this.userId = userId;
        this.status = status;
        this.name = name;
        this.nickname = nickname;
        this.dong = dong;
        this.ho = ho;
    }
}
