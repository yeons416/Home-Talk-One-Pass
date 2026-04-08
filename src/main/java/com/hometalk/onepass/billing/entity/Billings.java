package com.hometalk.onepass.billing.entity;

import com.hometalk.onepass.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/* 세대별 월 관리비 청구 테이블
 *
 * DB 제약조건:
 *  C1: UNIQUE(household_id, billing_month) - 동일 세대 동일 월 중복 고지 불가 (UPSERT 기준)
 *  C2: CHECK(total_amount >= 0)            - 음수 금액 입력 불가
 *  C4: ENUM(status: UNPAID, PAID)          - 정해진 상태 값 외 입력 불가
 *  C5: NOT NULL(household_id, billing_month, total_amount, due_date)
 *
 * 연관 관계:
 *  BillingDetails (1:N) - billing_id FK, CASCADE DELETE
 *  BillingLogs    (1:N) - billing_id FK
 */
@Entity
@Table(
        name = "billings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_billings_household_month",
                        columnNames = {"household_id", "billing_month"}
                )
        },
        indexes = {
                @Index(name = "idx_billings_household_id",  columnList = "household_id"),
                @Index(name = "idx_billings_billing_month", columnList = "billing_month"),
                @Index(name = "idx_billings_status",        columnList = "status"),
                @Index(name = "idx_billings_due_date",      columnList = "due_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Billings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /* FK - household.id 참조
     * - 입주민: Spring Security 로그인 사용자의 household_id로 본인 데이터만 조회
     * - 관리자: 미납 세대 목록 조회 시 필터 기준
     */
    @Column(name = "household_id", nullable = false)
    private Long householdId;

    /* 부과월 (예: "2026-03")
     * - 입주민/관리자 화면 연·월 드롭다운 필터 기준
     * - BETWEEN 쿼리: billing_month BETWEEN '2026-01' AND '2026-12'
     */
    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    /* 납부기한
     * - 입주민 고지서 모달: due_date 값 직접 노출 ("납부기한: 2026.03.31")
     * - 조건부 렌더링 분기:
     *     오늘 <= due_date → UNPAID 납기일 이전 안내
     *     오늘 >  due_date → UNPAID 납기일 경과 안내 ("납부기한이 지났습니다")
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /* 해당 월 청구 총액 (C2: CHECK >= 0)
     * - BillingDetails의 item_amount 합산 값
     * - 입주민: 요약 카드·목록 금액 표시
     * - 관리자: 미납 세대 목록·업로드 미리보기 금액 표시
     */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAmount;

    /* 납부 상태 (C4: ENUM UNPAID / PAID)
     * - UNPAID: 기본값. 엑셀 업로드 시 자동 설정
     * - PAID  : 관리자 '납부완료 처리' 확인 시 변경
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private BillingStatus status = BillingStatus.UNPAID;

    /* 1:N — BillingDetails
     * - 고지서 모달 상세 항목 리스트 조회 시 사용
     * - CASCADE ALL + orphanRemoval: 부모 삭제 시 상세항목 자동 삭제
     *   UPSERT UPDATE 시 서비스 레이어에서 Delete-Insert 처리
     */
    @Builder.Default
    @OneToMany(
            mappedBy = "billing",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<BillingDetails> billingDetails = new ArrayList<>();

    // ── 비즈니스 메서드 ──────────────────────────────────────────

    /* 관리자 납부완료 처리
     * - BillingLogs STATUS_CHANGE 기록은 서비스 레이어에서 처리
     */
    public void markAsPaid() {
        this.status = BillingStatus.PAID;
    }

    /* 고지서 업로드 UPSERT UPDATE — 기존 월 데이터 갱신
     * - total_amount, due_date를 최신 엑셀 데이터로 덮어씀
     * - status 유지 (이미 PAID 처리된 건 변경하지 않음)
     */
    public void updateByUpload(BigDecimal totalAmount, LocalDate dueDate) {
        this.totalAmount = totalAmount;
        this.dueDate     = dueDate;
    }
}