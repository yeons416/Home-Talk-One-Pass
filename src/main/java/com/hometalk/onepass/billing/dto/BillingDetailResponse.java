package com.hometalk.onepass.billing.dto;

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

import com.hometalk.onepass.billing.entity.BillingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class BillingDetailResponse {

    private final String billingMonth;      // "2026.02"
    private final String dongHo;            // "101동 1204호"
    private final LocalDate dueDate;        // 납부기한
    private final BillingStatus status;     // UNPAID / PAID
    private final LocalDate paidAt;         // 납부일 (PAID일 때만)
    private final BigDecimal totalAmount;   // 합계 금액
    private final List<ItemDetail> items;   // 상세 항목 리스트

    @Getter
    @Builder
    public static class ItemDetail {
        private final String itemName;
        private final BigDecimal itemAmount;
    }
}