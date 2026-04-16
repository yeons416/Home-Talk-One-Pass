package com.hometalk.onepass.billing.service;

import com.hometalk.onepass.billing.dto.BillingDetailResponse;
import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.dto.ResidentBillingResponse;
import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.billing.entity.BillingLog;
import com.hometalk.onepass.billing.entity.BillingActionType;
import com.hometalk.onepass.billing.repository.BillingDetailRepository;
import com.hometalk.onepass.billing.repository.BillingLogRepository;
import com.hometalk.onepass.billing.repository.BillingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingService {

    private final BillingRepository       billingRepository;
    private final BillingDetailRepository billingDetailRepository;
    private final BillingLogRepository    billingLogRepository;

    // ─────────────────────────────────────────────
    // 입주민: 관리비 페이지 초기 데이터
    // ─────────────────────────────────────────────

    public ResidentBillingResponse getResidentBillingPage(Long householdId) {

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // 미납 배너
        Billing latestUnpaid = billingRepository
                .findLatestUnpaidByHouseholdId(householdId)
                .orElse(null);
        boolean hasUnpaid        = latestUnpaid != null;
        String latestUnpaidMonth = hasUnpaid
                ? formatBillingMonthKo(latestUnpaid.getBillingMonth())
                : null;

        // 이번 달 청구액
        java.math.BigDecimal currentMonthAmount = billingRepository
                .findByHousehold_IdAndBillingMonth(householdId, currentMonth)
                .map(Billing::getTotalAmount)
                .orElse(null);

        // 미납 건수
        int unpaidCount = billingRepository
                .countByHousehold_IdAndStatus(householdId, BillingStatus.UNPAID);

        // 최근 납부일
        LocalDate lastPaidDate = billingLogRepository
                .findTopByBilling_Household_IdAndActionTypeOrderByCreatedAtDesc(
                        householdId, BillingActionType.STATUS_CHANGE)
                .map(log -> log.getCreatedAt().toLocalDate())
                .orElse(null);

        // 내역 리스트 최신 3건
        Pageable top3 = PageRequest.of(0, 3);
        List<BillingSummaryResponse> billings = billingRepository
                .findByHouseholdIdWithFilter(householdId, null, null, null, null, top3)
                .getContent()
                .stream()
                .map(BillingSummaryResponse::from)
                .toList();

        return ResidentBillingResponse.builder()
                .hasUnpaid(hasUnpaid)
                .latestUnpaidMonth(latestUnpaidMonth)
                .currentMonthAmount(currentMonthAmount)
                .unpaidCount(unpaidCount)
                .lastPaidDate(lastPaidDate)
                .billings(billings)
                .build();
    }

    // 입주민 목록 필터 조회
    public Page<BillingSummaryResponse> getBillingList(
            Long householdId,
            Integer year,
            String month,
            BillingStatus status,
            Pageable pageable
    ) {
        String yearFrom = year != null ? year + "-01" : null;
        String yearTo   = year != null ? year + "-12" : null;

        return billingRepository
                .findByHouseholdIdWithFilter(householdId, yearFrom, yearTo, month, status, pageable)
                .map(BillingSummaryResponse::from);
    }

    // ─────────────────────────────────────────────
    // 공용: 고지서 모달 상세 조회
    // ─────────────────────────────────────────────

    public BillingDetailResponse getBillingDetail(Long billingId) {

        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고지서입니다. id=" + billingId));

        List<BillingDetailResponse.ItemDetail> items = billingDetailRepository
                .findByBilling_IdOrderBySortOrderAsc(billingId)
                .stream()
                .map(d -> BillingDetailResponse.ItemDetail.builder()
                        .itemName(d.getItemName())
                        .itemAmount(d.getItemAmount())
                        .build())
                .toList();

        LocalDate paidAt = null;
        if (billing.getStatus() == BillingStatus.PAID) {
            paidAt = billingLogRepository
                    .findTopByBilling_IdAndActionTypeOrderByCreatedAtDesc(
                            billingId, BillingActionType.STATUS_CHANGE)
                    .map(log -> log.getCreatedAt().toLocalDate())
                    .orElse(null);
        }

        return BillingDetailResponse.builder()
                .billingMonth(billing.getBillingMonth().replace("-", "."))
                .dongHo(billing.getHousehold().getDong() + " " + billing.getHousehold().getHo())
                .dueDate(billing.getDueDate())
                .status(billing.getStatus())
                .paidAt(paidAt)
                .totalAmount(billing.getTotalAmount())
                .items(items)
                .build();
    }

    // ─────────────────────────────────────────────
    // 관리자: 납부완료 처리
    // ─────────────────────────────────────────────

    @Transactional
    public void markAsPaid(Long billingId, Long adminId) {

        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고지서입니다. id=" + billingId));

        if (billing.getStatus() == BillingStatus.PAID) {
            throw new IllegalStateException("이미 납부 완료 처리된 고지서입니다. id=" + billingId);
        }

        billing.markAsPaid();

        billingLogRepository.save(BillingLog.builder()
                .billing(billing)
                .userId(adminId)
                .actionType(BillingActionType.STATUS_CHANGE)
                .build());
    }
    // ─────────────────────────────────────────────
    // 관리자: 엑셀 업로드 중복시 팝업창
    // ─────────────────────────────────────────────

    public boolean existsByBillingMonth(String billingMonth) {
        return billingRepository.existsByBillingMonth(billingMonth);
    }

    // ─────────────────────────────────────────────
    // 관리자: 미납 세대 목록
    // ─────────────────────────────────────────────

    public Page<BillingSummaryResponse> getUnpaidList(
            String dong,
            Integer year,
            String month,
            BillingStatus status,
            boolean overdueMonths,
            Pageable pageable
    ) {
        String yearFrom      = year != null ? year + "-01" : null;
        String yearTo        = year != null ? year + "-12" : null;
        String overdueBefore = overdueMonths
                ? LocalDate.now().minusMonths(3)
                .format(DateTimeFormatter.ofPattern("yyyy-MM"))
                : null;

        return billingRepository
                .findAllWithAdminFilter(dong, yearFrom, yearTo, month, status, overdueBefore, pageable)
                .map(b -> BillingSummaryResponse.of(
                        b,
                        "-"
        //                b.getHousehold().getUsers().get(0).getName()  // 대표 입주민명
                        // TODO: HouseholdRepository 머지 후 b.getHousehold().getUsers().get(0).getName() 으로 교체
                ));

    }

    // 관리자 통계
    public AdminBillingStats getAdminStats(String billingMonth) {
        long total  = billingRepository.countDistinctHouseholdByBillingMonth(billingMonth);
        long paid   = billingRepository.countByBillingMonthAndStatus(billingMonth, BillingStatus.PAID);
        long unpaid = total - paid;
        double paidRate = total > 0 ? (double) paid / total * 100 : 0;
        return new AdminBillingStats(total, paid, unpaid, paidRate);
    }

    // 관리자: 고지서 전체 목록 조회
    public Page<BillingSummaryResponse> getAdminList(
            String dong,
            Integer year,
            String month,
            Pageable pageable
    ) {
        String yearFrom = year != null ? year + "-01" : null;
        String yearTo   = year != null ? year + "-12" : null;

        return billingRepository
                .findAllWithAdminFilter(dong, yearFrom, yearTo, month, null, null, pageable)
                .map(b -> BillingSummaryResponse.of(b, "-"));
    }

    public record AdminBillingStats(
            long total,
            long paid,
            long unpaid,
            double paidRate
    ) {}

    // ─────────────────────────────────────────────
    // 내부 유틸
    // ─────────────────────────────────────────────

    private String formatBillingMonthKo(String billingMonth) {
        String[] parts = billingMonth.split("-");
        return parts[0] + "년 " + Integer.parseInt(parts[1]) + "월";
    }
}