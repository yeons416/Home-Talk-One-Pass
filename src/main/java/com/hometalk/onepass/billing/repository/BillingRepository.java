package com.hometalk.onepass.billing.repository;

import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BillingRepository extends JpaRepository<Billing, Long> {

    // 입주민 목록 조회 (필터 + 페이징)
    @Query("""
            SELECT b FROM Billing b
            WHERE b.household.id = :householdId
              AND (:yearFrom IS NULL OR b.billingMonth >= :yearFrom)
              AND (:yearTo   IS NULL OR b.billingMonth <= :yearTo)
              AND (:month    IS NULL OR b.billingMonth = :month)
              AND (:status   IS NULL OR b.status = :status)
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

    // 이번 달 청구액 조회
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

    // 관리자 업로드 중복시 팝업 메서드
    boolean existsByBillingMonth(String billingMonth);

    // 관리자 미납 목록 (필터 + 페이징)
    @Query("""
            SELECT b FROM Billing b
            WHERE (:dong         IS NULL OR b.household.dong = :dong)
              AND (:yearFrom     IS NULL OR b.billingMonth >= :yearFrom)
              AND (:yearTo       IS NULL OR b.billingMonth <= :yearTo)
              AND (:month        IS NULL OR b.billingMonth = :month)
              AND (:status       IS NULL OR b.status = :status)
              AND (:overdueBefore IS NULL OR b.billingMonth <= :overdueBefore)
            ORDER BY b.billingMonth DESC
            """)
    Page<Billing> findAllWithAdminFilter(
            @Param("dong")          String dong,
            @Param("yearFrom")      String yearFrom,
            @Param("yearTo")        String yearTo,
            @Param("month")         String month,
            @Param("status")        BillingStatus status,
            @Param("overdueBefore") String overdueBefore,
            Pageable pageable
    );

    // 관리자 통계
    @Query("SELECT COUNT(DISTINCT b.household.id) FROM Billing b WHERE b.billingMonth = :billingMonth")
    long countDistinctHouseholdByBillingMonth(@Param("billingMonth") String billingMonth);

    long countByBillingMonthAndStatus(String billingMonth, BillingStatus status);

    // UPSERT 기준 조회
    //Optional<Billing> findByHousehold_IdAndBillingMonth(Long householdId, String billingMonth);
}