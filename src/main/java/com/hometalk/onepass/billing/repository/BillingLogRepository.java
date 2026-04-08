package com.hometalk.onepass.billing.repository;

import com.hometalk.onepass.billing.entity.BillingActionType;
import com.hometalk.onepass.billing.entity.BillingLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BillingLogRepository extends JpaRepository<BillingLogs, Long> {

    /* 최근 납부 완료 일시 조회
     * - 사용처: 입주민 요약 카드 "최근 납부일"
     * - billing_logs에서 STATUS_CHANGE 가장 최근 created_at 조회
     * - Billings에 paid_at 컬럼이 없으므로 여기서 조회
     */
    @Query("""
            SELECT l FROM BillingLogs l
            WHERE l.billing.householdId = :householdId
              AND l.actionType = :actionType
            ORDER BY l.createdAt DESC
            LIMIT 1
            """)
    Optional<BillingLogs> findLatestByHouseholdIdAndActionType(
            @Param("householdId") Long householdId,
            @Param("actionType")  BillingActionType actionType
    );

    /* 특정 고지서의 납부 완료 처리 일시
     * - 사용처: 입주민 고지서 모달 "납부일: YYYY.MM.DD" 표시
     * - BillingDetailResponse.paidAt 채우기 용도
     */
    @Query("""
            SELECT l FROM BillingLogs l
            WHERE l.billing.id = :billingId
              AND l.actionType = 'STATUS_CHANGE'
            ORDER BY l.createdAt DESC
            LIMIT 1
            """)
    Optional<BillingLogs> findLatestStatusChangeByBillingId(
            @Param("billingId") Long billingId
    );

    /* 업로드 로그 중복 체크
     * - 사용처: 동일 부과월 재업로드 여부 확인 (팝업 트리거)
     * - description에 billingMonth 포함 여부로 판별
     */
    @Query("""
            SELECT COUNT(l) > 0 FROM BillingLogs l
            WHERE l.actionType = 'UPLOAD'
              AND l.description LIKE CONCAT('%', :billingMonth, '%')
            """)
    boolean existsUploadLogByBillingMonth(
            @Param("billingMonth") String billingMonth
    );
}