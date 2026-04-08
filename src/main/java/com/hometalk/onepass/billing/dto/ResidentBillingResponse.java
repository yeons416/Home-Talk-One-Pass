package com.hometalk.onepass.billing.dto;

import com.hometalk.onepass.billing.entity.BillingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/*
 * 입주민 관리비 페이지 전체 응답 DTO
 *
 * 사용처:
 *  - 입주민 관리비 페이지 최초 진입 시 한 번에 응답
 *    · API: GET /hometop/api/billing (household_id는 Spring Security에서 추출)
 *
 * 화면 구성:
 *  - 상단 미납 경고 배너: hasUnpaid + latestUnpaidMonth 기준 노출
 *  - 요약 카드 3개: currentMonthAmount / unpaidCount / lastPaidDate
 *  - 관리비 내역 리스트: billings (BillingSummaryResponse 배열)
 */
@Getter
@Builder
public class ResidentBillingResponse {


    // 상단 미납 경고 배너

    /*
     * 미납 건 존재 여부
     * - true : 배너 노출 ("202X년 X월 관리비가 미납 상태입니다.")
     * - false: 배너 숨김
     */
    private final boolean hasUnpaid;

    /*
     * 가장 최근 미납 부과월 (예: "2026년 2월")
     * - 배너 문구 내 월 표시용
     * - hasUnpaid = false이면 null
     */
    private final String latestUnpaidMonth;


    // 요약 카드

    /*
     * 이번 달 청구액
     * - 현재 월 고지서의 total_amount
     * - 당월 고지서 미업로드 시 null
     */
    private final BigDecimal currentMonthAmount;

    /*
     * 미납 건수
     * - 해당 세대의 status = UNPAID인 고지서 총 건수
     */
    private final int unpaidCount;

    /*
     * 최근 납부일
     * - billing_logs에서 action_type = STATUS_CHANGE인 가장 최근 created_at
     * - 납부 이력 없을 시 null
     */
    private final LocalDate lastPaidDate;

    // 관리비 내역 리스트

    /*
     * 관리비 내역 리스트
     * - billing_month DESC 정렬 (최신순)
     * - 초기 노출: 최신 3건, '더보기' 클릭 시 추가 로드
     * - 각 항목: BillingSummaryResponse (부과월, 납기일, 금액, 상태, 고지서 보기 버튼)
     */
    private final List<BillingSummaryResponse> billings;
}