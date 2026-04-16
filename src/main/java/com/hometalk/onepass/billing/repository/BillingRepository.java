package com.hometalk.onepass.billing.repository;

import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BillingRepository extends JpaRepository<Billing, Long> {

    // ─────────────────────────────────────────────────────────
    // 입주민 목록 (필터 + 페이징)
    // ─────────────────────────────────────────────────────────

    @Query("""
            SELECT b FROM Billing b
            WHERE b.household.id = :householdId
              AND (:yearFrom IS NULL OR b.billingMonth >= :yearFrom)
              AND (:yearTo   IS NULL OR b.billingMonth <= :yearTo)
              AND (:month    IS NULL OR b.billingMonth  = :month)
              AND (:status   IS NULL OR b.status        = :status)
            ORDER BY b.billingMonth DESC
            """)
    Page<Billing> findByHouseholdIdWithFilter(
            @Param("householdId") Long householdId,
            @Param("yearFrom")    String yearFrom,
            @Param("yearTo")      String yearTo,
            @Param("month")       String month,
            @Param("status")      BillingStatus status,
            Pageable pageable
    );

    // 이번 달 청구서 단건
    Optional<Billing> findByHousehold_IdAndBillingMonth(Long householdId, String billingMonth);

    // 미납 건수
    int countByHousehold_IdAndStatus(Long householdId, BillingStatus status);

    // 최신 미납 1건
    @Query("""
            SELECT b FROM Billing b
            WHERE b.household.id = :householdId
              AND b.status = 'UNPAID'
            ORDER BY b.billingMonth DESC
            LIMIT 1
            """)
    Optional<Billing> findLatestUnpaidByHouseholdId(@Param("householdId") Long householdId);

    // 최신 납부완료 1건 (입주민 요약 카드: 최근 납부일 표시용)
    @Query("""
            SELECT b FROM Billing b
            WHERE b.household.id = :householdId
              AND b.status = 'PAID'
            ORDER BY b.billingMonth DESC
            LIMIT 1
            """)
    Optional<Billing> findLatestPaidByHouseholdId(@Param("householdId") Long householdId);

    // ─────────────────────────────────────────────────────────
    // 관리자 공통 목록 (필터 + 페이징)
    //   - 업로드 화면: status=null → 전체 조회
    //   - 미납 관리:   status=UNPAID → 미납만 조회
    //   - overdueBefore: 해당 월 이전 미납 → "3개월 이상 체납" 필터
    // ─────────────────────────────────────────────────────────

    @Query("""
            SELECT b FROM Billing b
            WHERE (:dong          IS NULL OR b.household.dong = :dong)
              AND (:yearFrom      IS NULL OR b.billingMonth  >= :yearFrom)
              AND (:yearTo        IS NULL OR b.billingMonth  <= :yearTo)
              AND (:month         IS NULL OR b.billingMonth   = :month)
              AND (:monthOnly     IS NULL OR SUBSTRING(b.billingMonth, 6, 2) = :monthOnly)
              AND (:status        IS NULL OR b.status         = :status)
              AND (:overdueBefore IS NULL OR b.billingMonth  <= :overdueBefore)
            ORDER BY b.billingMonth DESC, b.household.dong ASC, b.household.ho ASC
            """)
    Page<Billing> findAllWithAdminFilter(
            @Param("dong")          String dong,
            @Param("yearFrom")      String yearFrom,
            @Param("yearTo")        String yearTo,
            @Param("month")         String month,
            @Param("monthOnly")     String monthOnly,
            @Param("status")        BillingStatus status,
            @Param("overdueBefore") String overdueBefore,
            Pageable pageable
    );

    // ─────────────────────────────────────────────────────────
    // 관리자 통계
    // ─────────────────────────────────────────────────────────

    @Query("SELECT COUNT(DISTINCT b.household.id) FROM Billing b WHERE b.billingMonth = :billingMonth")
    long countDistinctHouseholdByBillingMonth(@Param("billingMonth") String billingMonth);

    long countByBillingMonthAndStatus(String billingMonth, BillingStatus status);

    // ─────────────────────────────────────────────────────────
    // 업로드 중복 확인
    // ─────────────────────────────────────────────────────────

    boolean existsByBillingMonth(String billingMonth);

    // ─────────────────────────────────────────────────────────
    // 납부완료 처리 (status UNPAID → PAID)
    // ─────────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE Billing b SET b.status = :status WHERE b.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") BillingStatus status);
}