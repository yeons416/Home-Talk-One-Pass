package com.hometalk.onepass.parking.entity;
import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
public class Payment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;
    /**
     * 👉 결제 대상 ID (주차, 티켓 등)
     */
    @Column(name = "target_id", nullable = false)
    private Long targetId;
    /**
     * 👉 결제 대상 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private PaymentTargetType targetType;
    /**
     * 결제 금액
     */
    @Column(nullable = false)
    private int amount;
    /**
     * 결제 방식
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;
    /**
     * 결제 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    public Payment(Long targetId, PaymentTargetType targetType, int amount, PaymentMethod method) {
        if (targetId == null) {
            throw new IllegalArgumentException("결제 대상 ID는 필수입니다.");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("결제 대상 타입은 필수입니다.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }
        if (method == null) {
            throw new IllegalArgumentException("결제 방식은 필수입니다.");
        }
        this.targetId = targetId;
        this.targetType = targetType;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.PENDING;
    }
    /**
     * 결제 완료
     */
    public void complete() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("대기 상태에서만 결제 완료 처리할 수 있습니다.");
        }
        this.status = PaymentStatus.PAID;
    }
    /**
     * 결제 실패
     */
    public void fail() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("대기 상태에서만 실패 처리할 수 있습니다.");
        }
        this.status = PaymentStatus.FAILED;
    }
    /**
     * 결제 취소
     */
    public void cancel() {
        if (this.status != PaymentStatus.PAID) {
            throw new IllegalStateException("결제 완료 상태에서만 취소할 수 있습니다.");
        }
        this.status = PaymentStatus.CANCELLED;
    }
    /**
     * 결제 대상 타입
     */
    public enum PaymentTargetType {
        PARKING,   // 주차 요금
        TICKET,    // 이용권 구매
        ETC        // 기타 (확장용)
    }
    public enum PaymentMethod {
        CARD,
        APP,
        ONSITE
    }
    public enum PaymentStatus {
        PENDING,
        PAID,
        FAILED,
        CANCELLED
    }
}