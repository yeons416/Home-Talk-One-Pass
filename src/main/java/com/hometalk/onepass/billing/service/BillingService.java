package com.hometalk.onepass.billing.service;

import com.hometalk.onepass.billing.dto.BillingDetailResponse;
import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingActionType;
import com.hometalk.onepass.billing.entity.BillingDetail;
import com.hometalk.onepass.billing.entity.BillingLog;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.billing.repository.BillingDetailRepository;
import com.hometalk.onepass.billing.repository.BillingLogRepository;
import com.hometalk.onepass.billing.repository.BillingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingRepository       billingRepository;
    private final BillingDetailRepository billingDetailRepository;
    private final BillingLogRepository    billingLogRepository;

    // ─────────────────────────────────────────────
    // 관리자: 고지서 목록 (업로드 화면 DB 모드)
    //   status=null → PAID+UNPAID 전체 반환
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BillingSummaryResponse> getAdminBillingList(
            Integer year, String month, String dong, int size, int page
    ) {
        // month("2026-03")가 있으면 year 필터 무시, month 단독 사용
        String yearFrom = (year != null && month == null) ? year + "-01" : null;
        String yearTo   = (year != null && month == null) ? year + "-12" : null;

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("billingMonth").descending()
                        .and(Sort.by("household.dong").ascending())
                        .and(Sort.by("household.ho").ascending()));

        return billingRepository
                .findAllWithAdminFilter(dong, yearFrom, yearTo, month,
                        null, null, null, pageable)
                .map(BillingSummaryResponse::from);
    }

    // ─────────────────────────────────────────────
    // 관리자: 미납 세대 목록
    //   overdueOnly=true → billingMonth <= 3개월 전까지만 (체납 기준)
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BillingSummaryResponse> getAdminUnpaidList(
            Integer year, String month, String dong,
            Boolean overdueOnly, int size, int page
    ) {
        String yearFrom      = (year != null && month == null) ? year + "-01" : null;
        String yearTo        = (year != null && month == null) ? year + "-12" : null;
        String overdueBefore = Boolean.TRUE.equals(overdueOnly)
                ? YearMonth.now().minusMonths(3).toString()   // "2025-12" 이전 = 3개월 이상 체납
                : null;

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("billingMonth").descending()
                        .and(Sort.by("household.dong").ascending())
                        .and(Sort.by("household.ho").ascending()));

        return billingRepository
                .findAllWithAdminFilter(dong, yearFrom, yearTo, month,
                        null, BillingStatus.UNPAID, overdueBefore, pageable)
                .map(b -> BillingSummaryResponse.of(b, "—")); // TODO: residentName → CustomUserDetails 연동 후 교체
    }

    // ─────────────────────────────────────────────
    // 관리자: 통계 (미납 세대 관리 상단 Summary Cards)
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminStats(String billingMonth) {
        long total  = billingRepository.countDistinctHouseholdByBillingMonth(billingMonth);
        long paid   = billingRepository.countByBillingMonthAndStatus(billingMonth, BillingStatus.PAID);
        long unpaid = billingRepository.countByBillingMonthAndStatus(billingMonth, BillingStatus.UNPAID);
        double rate = total > 0 ? Math.round((double) paid / total * 1000.0) / 10.0 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalHouseholds", total);
        stats.put("paidCount",       paid);
        stats.put("unpaidCount",     unpaid);
        stats.put("paymentRate",     rate);
        return stats;
    }

    // ─────────────────────────────────────────────
    // 관리자: 납부완료 처리
    //   billing_logs에 STATUS_CHANGE 기록 (paid_at은 로그 created_at으로 추적)
    // ─────────────────────────────────────────────

    @Transactional
    public void markAsPaid(Long billingId, Long adminId) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new EntityNotFoundException("Billing not found: " + billingId));

        if (billing.getStatus() == BillingStatus.PAID) return;  // 중복 처리 방지

        billingRepository.updateStatus(billingId, BillingStatus.PAID);

        billingLogRepository.save(BillingLog.builder()
                .billing(billing)
                .userId(adminId)
                .actionType(BillingActionType.STATUS_CHANGE)
                .build());
    }

    // ─────────────────────────────────────────────
    // 고지서 상세 (관리자 미리보기 + 입주민 고지서 모달 공통)
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BillingDetailResponse getBillingDetail(Long billingId) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new EntityNotFoundException("Billing not found: " + billingId));

        List<BillingDetail> details =
                billingDetailRepository.findByBilling_IdOrderBySortOrderAsc(billingId);

        return BillingDetailResponse.from(billing, details);
    }

    // ─────────────────────────────────────────────
    // 업로드: 부과월 중복 확인
    // ─────────────────────────────────────────────

    public boolean existsByBillingMonth(String billingMonth) {
        return billingRepository.existsByBillingMonth(billingMonth);
    }

    // ─────────────────────────────────────────────
    // 입주민: 관리비 내역 목록
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BillingSummaryResponse> getResidentBillingList(
            Long householdId, Integer year, String month,
            String statusStr, int size, int page
    ) {
        String yearFrom = (year != null && month == null) ? year + "-01" : null;
        String yearTo   = (year != null && month == null) ? year + "-12" : null;
        BillingStatus status = (statusStr != null && !statusStr.isBlank())
                ? BillingStatus.valueOf(statusStr) : null;

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("billingMonth").descending());

        return billingRepository
                .findByHouseholdIdWithFilter(householdId, yearFrom, yearTo, month, status, pageable)
                .map(BillingSummaryResponse::from);
    }

    // ─────────────────────────────────────────────
    // 입주민: 요약 카드 (이번 달 청구액 / 미납 건수 / 최근 납부일)
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getResidentSummary(Long householdId) {
        String currentMonth = YearMonth.now().toString();

        Optional<Billing> current = billingRepository
                .findByHousehold_IdAndBillingMonth(householdId, currentMonth);

        int unpaidCount = billingRepository
                .countByHousehold_IdAndStatus(householdId, BillingStatus.UNPAID);

        Optional<Billing> latestPaid = billingRepository
                .findLatestPaidByHouseholdId(householdId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("currentMonthAmount", current.map(Billing::getTotalAmount).orElse(null));
        summary.put("currentMonth",       currentMonth);
        summary.put("unpaidCount",        unpaidCount);
        // paid_at은 billing_logs.created_at 기준 — 여기서는 billingMonth로 대체 표시
        // TODO: BillingLogRepository에서 최신 STATUS_CHANGE 로그의 created_at 조회로 교체
        summary.put("latestPaidMonth",    latestPaid.map(Billing::getBillingMonth).orElse(null));
        return summary;
    }
}