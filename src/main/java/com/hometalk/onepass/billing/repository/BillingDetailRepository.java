package com.hometalk.onepass.billing.repository;

import com.hometalk.onepass.billing.entity.BillingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BillingDetailRepository extends JpaRepository<BillingDetails, Long> {

    /* 고지서 상세 항목 조회
     * - 사용처: 입주민/관리자 고지서 모달 (BillingModal)
     * - API: GET /hometop/api/billing/{billingId}/detail
     */
    List<BillingDetails> findByBillingIdOrderByIdAsc(Long billingId);

    /* UPSERT UPDATE 시 기존 상세항목 전체 삭제
     * - 사용처: 엑셀 업로드 UPDATE 케이스
     * - 삭제 후 최신 엑셀 데이터로 재삽입 (Delete-Insert)
     * - @Modifying + @Transactional 은 서비스 레이어에서 처리
     */
    @Modifying
    @Query("DELETE FROM BillingDetails d WHERE d.billing.id = :billingId")
    void deleteAllByBillingId(@Param("billingId") Long billingId);

    /* 특정 부과월 전체 상세항목 일괄 삭제
     * - 사용처: 업로드 취소 롤백 시 (향후 확장)
     */
    @Modifying
    @Query("""
            DELETE FROM BillingDetails d
            WHERE d.billing.id IN (
                SELECT b.id FROM Billings b WHERE b.billingMonth = :billingMonth
            )
            """)
    void deleteAllByBillingMonth(@Param("billingMonth") String billingMonth);
}