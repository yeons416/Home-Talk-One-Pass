package com.hometalk.onepass.billing.dto;
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

import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class BillingSummaryResponse {

    private final Long billingId;
    private final String dong;
    private final String ho;
    private final String unit;          // 추가 - "101동 1204호"
    private final String residentName;
    private final String billingMonth;
    private final LocalDate dueDate;
    private final BigDecimal totalAmount;
    private final BillingStatus status;

    // 입주민 목록용
    public static BillingSummaryResponse from(Billing billing) {
        return BillingSummaryResponse.builder()
                .billingId(billing.getId())
                .dong(billing.getHousehold().getDong())
                .ho(billing.getHousehold().getHo())
                .unit(billing.getHousehold().getDong() + " " + billing.getHousehold().getHo())
                .billingMonth(billing.getBillingMonth())
                .dueDate(billing.getDueDate())
                .totalAmount(billing.getTotalAmount())
                .status(billing.getStatus())
                .build();
    }

    // 관리자 미납 목록용 (입주민명 포함)
    public static BillingSummaryResponse of(Billing billing, String residentName) {
        return BillingSummaryResponse.builder()
                .billingId(billing.getId())
                .dong(billing.getHousehold().getDong())
                .ho(billing.getHousehold().getHo())
                .unit(billing.getHousehold().getDong() + " " + billing.getHousehold().getHo())
                .residentName(residentName)
                .billingMonth(billing.getBillingMonth())
                .dueDate(billing.getDueDate())
                .totalAmount(billing.getTotalAmount())
                .status(billing.getStatus())
                .build();
    }
}