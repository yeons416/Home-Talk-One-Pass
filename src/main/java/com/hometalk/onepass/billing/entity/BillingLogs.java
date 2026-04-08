package com.hometalk.onepass.billing.entity;

import com.hometalk.onepass.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/* 관리비 처리 이력 로그 테이블
 *
 * 기록 대상 Action:
 *  - UPLOAD        : 관리자 엑셀 업로드 확정 시
 *  - STATUS_CHANGE : 관리자 납부완료 처리 시 (UNPAID → PAID)
 *
 * DB 제약조건:
 *  C3: ON DELETE SET NULL (user_id) - 관리자 탈퇴 시에도 로그 이력 보존
 *  C5: NOT NULL(billing_id, action_type)
 *
 * 연관 관계:
 *  Billings (N:1) - billing_id FK
 *  Users    (N:1) - user_id FK, SET NULL (관리자 탈퇴 이력 보존)
 */
@Entity
@Table(
        name = "billing_logs",
        indexes = {
                @Index(name = "idx_billing_logs_billing_id",  columnList = "billing_id"),
                @Index(name = "idx_billing_logs_user_id",     columnList = "user_id"),
                @Index(name = "idx_billing_logs_action_type", columnList = "action_type"),
                @Index(name = "idx_billing_logs_created_at",  columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BillingLogs extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /* FK - 로그 대상 청구 건 (billings.id 참조) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "billing_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_billing_logs_billing_id")
    )
    private Billings billing;

    /* 작업 수행 관리자 ID (user.id 참조)
     * - C3: ON DELETE SET NULL — 관리자 탈퇴 시 user_id를 NULL로 유지하여 로그 보존
     * - nullable = true (SET NULL 대응)
     */
    @Column(name = "user_id", nullable = true)
    private Long userId;

    /* 액션 타입
     * - UPLOAD        : 엑셀 업로드 확정 (module_name=BILLING, category=NEW 알림 트리거)
     * - STATUS_CHANGE : 납부 상태 변경 (UNPAID → PAID, 입주민 알림 트리거)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private BillingActionType actionType;
}