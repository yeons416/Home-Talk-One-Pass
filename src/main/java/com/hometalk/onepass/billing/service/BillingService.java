package com.hometalk.onepass.billing.service;
/*
 조회 / 납부완료 처리
* */

import com.hometalk.onepass.billing.dto.BillingDetailResponse;
import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.dto.ResidentBillingResponse;
import com.hometalk.onepass.billing.entity.*;
import com.hometalk.onepass.billing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingService {

    private final BillingsRepository       billingsRepository;
    private final BillingDetailsRepository billingDetailsRepository;
    private final BillingLogsRepository    billingLogsRepository;

    // ═══════════════════════════════════════════════════════
    // 입주민 — 관리비 페이지 초기 진입
    // ═══════════════════════════════════════════════════════

    /**
     * 입주민 관리비 페이지 전체 데이터 조회
     * - 미납 배너, 요약 카드 3개, 목록 초기 3건 한 번에 반환
     * - 사용처: BillingPageController.residentBillingPage()
     */
    public ResidentBillingResponse getResidentBillingPage(Long householdId) {

        // 전체 목록 (최신순)
        List<Billings> all = billingsRepository
                .findByHouseholdIdWithFilter(householdId, null, null, null);

        // 미납 목록
        List<Billings> unpaidList = all.stream()
                .filter(b -> b.getStatus() == BillingStatus.UNPAID)
                .toList();

        // 이번 달 고지서
        String currentMonth = YearMonth.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Billings currentMonthBilling = all.stream()
                .filter(b -> b.getBillingMonth().equals(currentMonth))
                .findFirst().orElse(null);

        // 최근 납부일 (billing_logs STATUS_CHANGE 기준)
        Optional<BillingLogs> latestLog = billingLogsRepository
                .findLatestByHouseholdIdAndActionType(householdId, BillingActionType.STATUS_CHANGE);
        LocalDate lastPaidDate  = latestLog.map(l -> l.getCreatedAt().toLocalDate()).orElse(null);
        String    lastPaidMonth = latestLog.map(l -> {
            String m = l.getBilling().getBillingMonth(); // "2026-01"
            return m.replace("-", "년 ") + "월";
        }).orElse(null);

        // 미납 월 목록 (배너·카드 표시용)
        List<String> unpaidMonths = unpaidList.stream()
                .map(b -> b.getBillingMonth().replace("-", "년 ") + "월")
                .toList();

        // 초기 목록 3건 + hasMore
        List<BillingSummaryResponse> billings = all.stream()
                .limit(3)
                .map(b -> toBillingSummary(b, householdId))
                .toList();

        return ResidentBillingResponse.builder()
                .hasUnpaid(            !unpaidList.isEmpty())
                .latestUnpaidMonth(    unpaidMonths.isEmpty() ? null : unpaidMonths.get(0))
                .currentMonthBilling(  currentMonthBilling != null
                        ? toBillingSummary(currentMonthBilling, householdId) : null)
                .currentMonthAmount(   currentMonthBilling != null
                        ? currentMonthBilling.getTotalAmount() : null)
                .unpaidCount(          unpaidList.size())
                .unpaidMonths(         unpaidMonths)
                .unpaidBillings(       unpaidList.stream()
                        .map(b -> toBillingSummary(b, householdId)).toList())
                .lastPaidDate(         lastPaidDate)
                .lastPaidMonth(        lastPaidMonth)
                .billings(             billings)
                .hasMore(              all.size() > 3)
                .build();
    }

    /**
     * 입주민 관리비 목록 필터 재조회
     * - 사용처: BillingApiController.getBillingList() (JS fetch)
     */
    public List<BillingSummaryResponse> getBillingList(
            Long householdId, String year, String month, BillingStatus status) {

        String paddedMonth = (month != null && month.length() == 1) ? "0" + month : month;

        return billingsRepository
                .findByHouseholdIdWithFilter(householdId, year, paddedMonth, status)
                .stream()
                .map(b -> toBillingSummary(b, householdId))
                .toList();
    }

    /**
     * 고지서 모달 상세 조회
     * - 사용처: BillingApiController.getBillingDetail()
     * - 입주민 본인 고지서인지 검증 후 반환
     */
    public BillingDetailResponse getBillingDetail(Long billingId, Long householdId) {

        Billings billing = billingsRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("고지서를 찾을 수 없습니다. id=" + billingId));

        // 입주민 본인 고지서 검증 (관리자는 householdId = null 로 넘겨 skip 가능)
        if (householdId != null && !billing.getHouseholdId().equals(householdId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // 납부일 조회 (billing_logs STATUS_CHANGE 기준)
        LocalDate paidAt = null;
        if (billing.getStatus() == BillingStatus.PAID) {
            paidAt = billingLogsRepository
                    .findLatestStatusChangeByBillingId(billingId)
                    .map(l -> l.getCreatedAt().toLocalDate())
                    .orElse(null);
        }

        // 상세 항목
        List<BillingDetailResponse.ItemDetail> items =
                billingDetailsRepository.findByBillingIdOrderByIdAsc(billingId)
                        .stream()
                        .map(d -> BillingDetailResponse.ItemDetail.builder()
                                .itemName(d.getItemName())
                                .itemAmount(d.getItemAmount())
                                .build())
                        .toList();

        // 동/호 조회
        // TODO: HouseholdRepository 연동 후 실제 동/호 값으로 교체
        String dongHo = getDongHo(billing.getHouseholdId());

        return BillingDetailResponse.builder()
                .billingMonth(billing.getBillingMonth()
                        .replace("-", "."))           // "2026-02" → "2026.02"
                .dongHo(dongHo)
                .dueDate(billing.getDueDate())
                .status(billing.getStatus())
                .paidAt(paidAt)
                .totalAmount(billing.getTotalAmount())
                .items(items)
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // 관리자 — 미납 세대 관리
    // ═══════════════════════════════════════════════════════

    /**
     * 관리자 미납 세대 목록 (페이지네이션)
     * - 사용처: BillingPageController.adminUnpaidPage(), BillingApiController.getAdminUnpaidList()
     */
    public Page<BillingSummaryResponse> getAdminUnpaidList(
            BillingStatus status, String year, String month,
            String dong, Pageable pageable) {

        String paddedMonth = (month != null && month.length() == 1) ? "0" + month : month;

        Page<Billings> page = billingsRepository
                .findForAdminUnpaid(status, year, paddedMonth, pageable);

        List<BillingSummaryResponse> content = page.getContent().stream()
                .map(b -> toAdminBillingSummary(b))
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    /**
     * 3개월 이상 체납 목록
     */
    public Page<BillingSummaryResponse> getLongOverdueList(Pageable pageable) {
        List<Billings> all = billingsRepository.findLongOverdueHouseholds();
        List<BillingSummaryResponse> content = all.stream()
                .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .map(b -> toAdminBillingSummary(b))
                .toList();
        return new PageImpl<>(content, pageable, all.size());
    }

    /**
     * 납부완료 처리 (UNPAID → PAID)
     * - billings.status 변경
     * - billing_logs STATUS_CHANGE 기록
     * - 사용처: BillingApiController.markAsPaid()
     */
    @Transactional
    public void markAsPaid(Long billingId, Long adminUserId) {

        Billings billing = billingsRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("고지서를 찾을 수 없습니다. id=" + billingId));

        if (billing.getStatus() == BillingStatus.PAID) {
            throw new IllegalStateException("이미 납부완료 처리된 고지서입니다.");
        }

        // 상태 변경
        billing.markAsPaid();

        // 로그 기록
        BillingLogs log = BillingLogs.builder()
                .billing(billing)
                .userId(adminUserId)
                .actionType(BillingActionType.STATUS_CHANGE)
                .build();
        billingLogsRepository.save(log);

        // TODO: 입주민 납부 완료 알림 생성 (NotificationService 연동)
    }

    // ═══════════════════════════════════════════════════════
    // 통계 카드 (관리자 미납 세대 관리 상단)
    // ═══════════════════════════════════════════════════════

    public long getTotalCount(String billingMonth) {
        BillingsRepository.BillingStatProjection stat =
                billingsRepository.countStatsByBillingMonth(billingMonth);
        return stat != null && stat.getTotalCount() != null ? stat.getTotalCount() : 0L;
    }

    public long getPaidCount(String billingMonth) {
        BillingsRepository.BillingStatProjection stat =
                billingsRepository.countStatsByBillingMonth(billingMonth);
        return stat != null && stat.getPaidCount() != null ? stat.getPaidCount() : 0L;
    }

    public long getUnpaidCount(String billingMonth) {
        BillingsRepository.BillingStatProjection stat =
                billingsRepository.countStatsByBillingMonth(billingMonth);
        return stat != null && stat.getUnpaidCount() != null ? stat.getUnpaidCount() : 0L;
    }

    public double getPaidRate(String billingMonth) {
        BillingsRepository.BillingStatProjection stat =
                billingsRepository.countStatsByBillingMonth(billingMonth);
        if (stat == null || stat.getTotalCount() == null || stat.getTotalCount() == 0) return 0.0;
        return Math.round((double) stat.getPaidCount() / stat.getTotalCount() * 1000) / 10.0;
    }

    // ═══════════════════════════════════════════════════════
    // 유틸
    // ═══════════════════════════════════════════════════════

    /** 세대 정보 문자열 반환 (예: "101동 1204호")
     *  TODO: HouseholdRepository 연동 후 실제 값으로 교체 */
    public String getUnitInfo(Long householdId) {
        // TODO: householdRepository.findById(householdId) → dong + "동 " + ho + "호"
        return "101동 1204호";
    }

    private String getDongHo(Long householdId) {
        // TODO: householdRepository.findById(householdId) → dong + "동 " + ho + "호"
        return "101동 1204호";
    }

    /** Billings → BillingSummaryResponse (입주민용) */
    private BillingSummaryResponse toBillingSummary(Billings b, Long householdId) {
        // TODO: household JOIN 으로 dong/ho 실제 값으로 교체
        return BillingSummaryResponse.from(b, "101동", "1204호");
    }

    /** Billings → BillingSummaryResponse (관리자용, 입주민명 포함) */
    private BillingSummaryResponse toAdminBillingSummary(Billings b) {
        // TODO: household JOIN 으로 dong/ho/residentName 실제 값으로 교체
        return BillingSummaryResponse.of(b, "101동", "1204호", "홍길동");
    }
}