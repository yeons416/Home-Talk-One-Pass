package com.hometalk.onepass.billing.dto;

import com.hometalk.onepass.billing.entity.BillingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/*
 * 고지서 모달 전체 응답 DTO
 *
 * 사용처:
 *  - 입주민 디지털 고지서 모달 (BillingModal - mode: 'resident')
 *    · API: GET /hometop/api/billing/{billingId}/detail
 *  - 관리자 업로드 미리보기 모달 (BillingModal - mode: 'preview')
 *    · 엑셀 파싱 데이터를 previewData props로 직접 전달 (API 미호출)
 *
 * 납부 상태 조건부 렌더링:
 *  - UNPAID + 오늘 <= dueDate : 납기일 이전 안내 (주황색 박스)
 *  - UNPAID + 오늘 >  dueDate : 납기일 경과 안내 (빨간색 박스)
 *  - PAID                    : 납부 완료 메시지 + paidAt 표시 (초록색 박스)
 */
@Getter
@Builder
public class BillingDetailResponse {

    /* 부과월 (예: "2026.02") - 모달 헤더 표시 */
    private final String billingMonth;

    /* 동/호 (예: "101동 1204호") - 모달 헤더 표시 */
    private final String dongHo;

    /* 납부기한 - 조건부 렌더링 분기 기준 및 "납부기한: YYYY.MM.DD" 표시 */
    private final LocalDate dueDate;

    /*
     * 납부 상태
     * - UNPAID: 미납 안내 박스 렌더링
     * - PAID  : 납부 완료 메시지 렌더링
     */
    private final BillingStatus status;

    /*
     * 납부 완료 일시
     * - PAID일 때만 값 존재, UNPAID이면 null
     * - DB에 컬럼 없음 → 서비스 레이어에서 billing_logs의
     *   action_type = STATUS_CHANGE인 created_at으로 채움
     * - 화면 표시: "납부일: 2026.03.15"
     */
    private final LocalDate paidAt;

    /* 청구 총액 - 모달 하단 합계 굵은 글씨 표시 */
    private final BigDecimal totalAmount;

    /* 상세 항목 리스트 - itemName / itemAmount 리스트 렌더링 */
    private final List<ItemDetail> items;

    // 중첩 클래스

    /*
     * 고지서 항목 상세
     * - itemName  : 일반관리비, 청소비, 전기료, 수도료, 난방비 등
     * - itemAmount: 항목별 금액
     */
    @Getter
    @Builder
    public static class ItemDetail {
        private final String itemName;
        private final BigDecimal itemAmount;
    }
}