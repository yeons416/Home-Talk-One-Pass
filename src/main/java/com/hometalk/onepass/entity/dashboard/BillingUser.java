package com.hometalk.onepass.entity.dashboard;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class BillingUser {

    @Id // 1. PK로 지정
    // @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 생성이 필요하다면 추가
    private Long userId;

    @Enumerated(EnumType.STRING) // 2. Enum 타입 처리 (STATUS가 enum인 경우)
    private STATUS status;

    private String billingMonth;

    // 3. 배열 타입은 @ElementCollection 등을 사용해야 합니다.
    // 간단한 해결을 위해 우선 필드 타입을 확인하거나 리스트 형태로 변경을 고려해보세요.
    @ElementCollection
    private List<String> itemName;

    @ElementCollection
    private List<Integer> itemAmount;

    private int totalAmount;
    private LocalDateTime dueDate;

    @Builder
    public BillingUser(Long userId, STATUS status, String billingMonth, List<String> itemName, List<Integer> itemAmount,
                       int totalAmount, LocalDateTime dueDate) {
        this.userId = userId;
        this.status = status;
        this.billingMonth = billingMonth;
        this.itemName = itemName;
        this.itemAmount = itemAmount;
        this.totalAmount = totalAmount;
        this.dueDate = dueDate;
    }
}
