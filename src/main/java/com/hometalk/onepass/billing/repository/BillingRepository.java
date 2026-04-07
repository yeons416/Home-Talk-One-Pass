package com.hometalk.onepass.billing.repository;

import com.hometalk.onepass.billing.entity.Billings;
import com.hometalk.onepass.billing.entity.BillingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillingRepository extends JpaRepository<Billings, Long> {

    // ═══════════════════════════════════════════════════════
    // 입주민 — 관리비 조회
    // ═══════════════════════════════════════════════════════

    /* 입주민 관리비 목록 (최신순)
     * - 사용처: 입주민 관리비 페이지 초기 진입, 필터 변경 시 재조회
     * - 필터: householdId 고정, year/month/status 선택적 적용
     * - billing_month DESC 정렬
     */
    @Query("""
            SELECT b FROM Billings b
            WHERE b.householdId = :householdId
              AND (:year   IS NULL OR b.billingMonth LIKE CONCAT(:year, '%'))
              AND (:month  IS NULL OR b.billingMonth = CONCAT(:year, '-', :month))
              AND (:status IS NULL OR b.status = :status)
            ORDER BY b.billingMonth DESC
            """)
    List<Billings> findByHouseholdIdWithFilter(
            @Param("householdId") Long householdId,
            @Param("year")        String year,
            @Param("month")       String month,
            @Param("status")      BillingStatus status
    );

    /* 입주민 미납 목록
     * - 사용처: 상단 미납 배너, 요약 카드 미납 건수
     */
    List<Billings> findByHouseholdIdAndStatusOrderByBillingMonthDesc(
            Long householdId,
            BillingStatus status
    );

    /* 이번 달 고지서 1건
     * - 사용처: 요약 카드 "이번 달 청구액"
     */
    Optional<Billings> findByHouseholdIdAndBillingMonth(
            Long householdId,
            String billingMonth
    );

    /* UPSERT 중복 체크
     * - 사용처: 엑셀 업로드 시 household_id + billing_month 기준 존재 여부 확인
     */
    Optional<Billings> findByHouseholdIdAndBillingMonth(
            Long householdId,
            String billingMonth,
            // 오버로딩 불가 → 아래 메서드와 이름 구분 필요하므로 @Query 사용
            @Param("dummy") Void dummy  // 사용하지 않음, 아래 메서드로 대체
    );

    // 위 오버로딩 대신 명시적 메서드명 사용
    boolean existsByHouseholdIdAndBillingMonth(
            Long householdId,
            String billingMonth
    );


    // ═══════════════════════════════════════════════════════
    // 관리자 — 미납 세대 관리
    // ═══════════════════════════════════════════════════════

    /* 미납 세대 목록 (페이지네이션)
     * - 사용처: 관리자 미납 세대 관리 페이지
     * - 필터: year, month, dong(building_name+dong), 미납만/전체/3개월이상
     * - household JOIN으로 동/호/입주민명 함께 조회
     */
    @Query("""
            SELECT b FROM Billings b
            JOIN FETCH b.billingDetails
            WHERE (:status IS NULL OR b.status = :status)
              AND (:year   IS NULL OR b.billingMonth LIKE CONCAT(:year, '%'))
              AND (:month  IS NULL OR b.billingMonth = CONCAT(:year, '-', :month))
            ORDER BY b.billingMonth DESC, b.householdId ASC
            """)
    Page<Billings> findForAdminUnpaid(
            @Param("status") BillingStatus status,
            @Param("year")   String year,
            @Param("month")  String month,
            Pageable pageable
    );

    /* 3개월 이상 체납 세대
     * - 사용처: 관리자 미납 세대 필터 "3개월 이상 체납"
     * - 동일 household_id 에서 UNPAID 건이 3개 이상인 세대만
     */
    @Query("""
            SELECT b FROM Billings b
            WHERE b.status = 'UNPAID'
              AND b.householdId IN (
                  SELECT b2.householdId FROM Billings b2
                  WHERE b2.status = 'UNPAID'
                  GROUP BY b2.householdId
                  HAVING COUNT(b2.id) >= 3
              )
            ORDER BY b.billingMonth DESC
            """)
    List<Billings> findLongOverdueHouseholds();

    /* 전체 세대 수 / 납부완료 수 / 미납 수 (통계 카드용)
     * - 사용처: 관리자 미납 세대 관리 상단 통계 카드 4개
     * - 특정 부과월 기준으로 집계
     */
    @Query("""
            SELECT
                COUNT(b)                                        AS totalCount,
                SUM(CASE WHEN b.status = 'PAID'   THEN 1 ELSE 0 END) AS paidCount,
                SUM(CASE WHEN b.status = 'UNPAID' THEN 1 ELSE 0 END) AS unpaidCount
            FROM Billings b
            WHERE b.billingMonth = :billingMonth
            """)
    BillingStatProjection countStatsByBillingMonth(
            @Param("billingMonth") String billingMonth
    );

    /* 납부율 계산용 인터페이스 프로젝션 */
    interface BillingStatProjection {
        Long getTotalCount();
        Long getPaidCount();
        Long getUnpaidCount();
    }


    // ═══════════════════════════════════════════════════════
    // 관리자 — 고지서 업로드
    // ═══════════════════════════════════════════════════════

    /* 특정 부과월 전체 조회
     * - 사용처: 업로드 확정 전 UPSERT 판별 (INSERT vs UPDATE)
     * - household_id 리스트 기준 일괄 조회로 N+1 방지
     */
    @Query("""
            SELECT b FROM Billings b
            WHERE b.billingMonth = :billingMonth
              AND b.householdId IN :householdIds
            """)
    List<Billings> findByBillingMonthAndHouseholdIdIn(
            @Param("billingMonth")  String billingMonth,
            @Param("householdIds")  List<Long> householdIds
    );

    /* 특정 부과월 전체 조회 (동 필터)
     * - 사용처: 업로드 후 테이블 필터링
     */
    @Query("""
            SELECT b FROM Billings b
            WHERE b.billingMonth = :billingMonth
            ORDER BY b.householdId ASC
            """)
    List<Billings> findAllByBillingMonth(
            @Param("billingMonth") String billingMonth
    );
}