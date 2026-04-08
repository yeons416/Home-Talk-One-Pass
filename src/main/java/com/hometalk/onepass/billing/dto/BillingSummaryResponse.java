package com.hometalk.onepass.billing.dto;

import com.hometalk.onepass.billing.entity.Billings;
import com.hometalk.onepass.billing.entity.BillingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/*
 * 관리비 목록 행 1건 응답 DTO
 *
 * 사용처:
 *  - 입주민 관리비 내역 리스트
 *    · 부과월, 납부기한(UNPAID 배지), 청구금액, status 배지, 고지서 보기 버튼
 *  - 관리자 미납 세대 관리 목록
 *    · 동/호, 입주민명, 부과월, 청구금액, 납기일, status, 납부완료 처리 버튼
 *  - 관리자 고지서 업로드 유효성 검사 + 미리보기 테이블
 *    · NUM, 동/호, household_id, 부과월, 청구금액, 검증상태, 비고(UPSERT)
 */
@Getter
@Builder
public class BillingSummaryResponse {

    /* 청구 고유번호 - 고지서 보기 클릭 시 billingId 전달 용도 */
    private final Long billingId;

    /* 소속 세대 ID */
    private final Long householdId;

    /* 동 (예: "101동") */
    private final String dong;

    /* 호 (예: "1204호") */
    private final String ho;

    /* 입주민 실명 - 관리자 미납 목록 노출용 */
    private final String residentName;

    /* 부과월 (예: "2026-03") */
    private final String billingMonth;

    /* 납부기한 - 목록 내 납부기한 표시 및 UNPAID 배지 조건 */
    private final LocalDate dueDate;

    /* 청구 총액 */
    private final BigDecimal totalAmount;

    /*
     * 납부 상태
     * - UNPAID: 주황색 배지, 미납 배너 조건
     * - PAID  : 초록색 배지
     */
    private final BillingStatus status;

    // 정적 팩토리 메서드

    /*
     * 입주민 본인 목록 조회용
     * - dong, ho, residentName은 서비스 레이어에서 households JOIN으로 채움
     */
    public static BillingSummaryResponse from(Billings billings, String dong, String ho) {
        return BillingSummaryResponse.builder()
                .billingId(billings.getId())
                .householdId(billings.getHouseholdId())
                .dong(dong)
                .ho(ho)
                .billingMonth(billings.getBillingMonth())
                .dueDate(billings.getDueDate())
                .totalAmount(billings.getTotalAmount())
                .status(billings.getStatus())
                .build();
    }

    /*
     * 관리자 미납 목록 조회용 - 입주민명 포함
     */
    public static BillingSummaryResponse of(
            Billings billings,
            String dong,
            String ho,
            String residentName
    ) {
        return BillingSummaryResponse.builder()
                .billingId(billings.getId())
                .householdId(billings.getHouseholdId())
                .dong(dong)
                .ho(ho)
                .residentName(residentName)
                .billingMonth(billings.getBillingMonth())
                .dueDate(billings.getDueDate())
                .totalAmount(billings.getTotalAmount())
                .status(billings.getStatus())
                .build();
    }
}