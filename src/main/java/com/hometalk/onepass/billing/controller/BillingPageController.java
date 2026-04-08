package com.hometalk.onepass.billing.controller;

import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.dto.ResidentBillingResponse;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/billing")
@RequiredArgsConstructor
public class BillingPageController {

    private final BillingService billingService;

    private static final int PAGE_SIZE = 10;

    // ═══════════════════════════════════════════════════════
    // 입주민 — 관리비 페이지
    // GET /hometop/billing
    // template: billing/resident
    // ═══════════════════════════════════════════════════════
    @GetMapping
    public String residentBillingPage(Model model) {

        Long householdId = getHouseholdId();

        // 페이지 초기 진입 시 전체 데이터 한 번에 조회
        ResidentBillingResponse response = billingService.getResidentBillingPage(householdId);

        // ── 미납 배너 ──────────────────────────────────────
        model.addAttribute("unpaidList",    response.getUnpaidBillings());
        model.addAttribute("hasUnpaid",     response.isHasUnpaid());

        // ── 요약 카드 ──────────────────────────────────────
        model.addAttribute("currentMonthBilling", response.getCurrentMonthBilling());
        model.addAttribute("currentMonthLabel",
                YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy년 M월")));
        model.addAttribute("unpaidCount",   response.getUnpaidCount());
        model.addAttribute("unpaidMonths",  response.getUnpaidMonths());
        model.addAttribute("latestPaidDate",  response.getLastPaidDate());
        model.addAttribute("latestPaidMonth", response.getLastPaidMonth());

        // ── 관리비 목록 (최신 3건 초기 노출) ──────────────
        model.addAttribute("billings",  response.getBillings());
        model.addAttribute("hasMore",   response.isHasMore());

        // ── 세대 정보 (JS 변수로 노출) ────────────────────
        model.addAttribute("householdId", householdId);
        model.addAttribute("unitInfo",    billingService.getUnitInfo(householdId));

        return "billing/resident";
    }

    // ═══════════════════════════════════════════════════════
    // 관리자 — 고지서 업로드 페이지
    // GET /hometop/admin/billing/upload
    // template: billing/admin_upload
    // ═══════════════════════════════════════════════════════
    @GetMapping("/admin/upload")
    public String adminUploadPage(Model model) {
        model.addAttribute("activeMenu",    "billing");
        model.addAttribute("activeSubMenu", "upload");
        return "billing/admin_upload";
    }

    // ═══════════════════════════════════════════════════════
    // 관리자 — 미납 세대 관리 페이지
    // GET /hometop/admin/billing/unpaid
    // template: billing/admin_unpaid
    // ═══════════════════════════════════════════════════════
    @GetMapping("/admin/unpaid")
    public String adminUnpaidPage(
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(required = false)      String year,
            @RequestParam(required = false)      String month,
            @RequestParam(required = false)      String dong,
            @RequestParam(required = false)      String filter,  // "unpaid"|"long"|"paid"|null=전체
            Model model
    ) {
        // 기본값: 현재 연월
        String currentYear  = (year  != null) ? year  : String.valueOf(LocalDate.now().getYear());
        String currentMonth = (month != null) ? month : null;

        // 필터 → BillingStatus 변환
        BillingStatus statusFilter = switch (filter != null ? filter : "") {
            case "unpaid" -> BillingStatus.UNPAID;
            case "paid"   -> BillingStatus.PAID;
            default       -> null; // 전체 or 3개월이상(별도처리)
        };
        boolean longOverdue = "long".equals(filter);

        PageRequest pageable = PageRequest.of(page, PAGE_SIZE);

        // 통계 카드
        String statMonth = currentMonth != null
                ? currentYear + "-" + currentMonth
                : currentYear + "-" + String.format("%02d", LocalDate.now().getMonthValue());

        model.addAttribute("activeMenu",    "billing");
        model.addAttribute("activeSubMenu", "unpaid");
        model.addAttribute("totalCount",  billingService.getTotalCount(statMonth));
        model.addAttribute("paidCount",   billingService.getPaidCount(statMonth));
        model.addAttribute("unpaidCount", billingService.getUnpaidCount(statMonth));
        model.addAttribute("paidRate",    billingService.getPaidRate(statMonth));

        // 목록
        Page<BillingSummaryResponse> pageResult = longOverdue
                ? billingService.getLongOverdueList(pageable)
                : billingService.getAdminUnpaidList(statusFilter, currentYear, currentMonth, dong, pageable);

        model.addAttribute("billingList",   pageResult.getContent());
        model.addAttribute("currentPage",   pageResult.getNumber());
        model.addAttribute("totalPages",    pageResult.getTotalPages());
        model.addAttribute("pageSize",      PAGE_SIZE);

        // 필터 현재 선택값 (화면 복원용)
        model.addAttribute("selYear",   currentYear);
        model.addAttribute("selMonth",  currentMonth);
        model.addAttribute("selDong",   dong);
        model.addAttribute("selFilter", filter);

        return "billing/admin_unpaid";
    }

    // ═══════════════════════════════════════════════════════
    // 내부 유틸
    // ═══════════════════════════════════════════════════════

    /**
     * 로그인 사용자의 householdId 추출
     *
     * TODO : Security 커스텀 UserDetails 완성 후 아래로 교체
     *   @AuthenticationPrincipal CustomUserDetails userDetails
     *   return userDetails.getHouseholdId();
     */
    private Long getHouseholdId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 임시: principal에서 householdId 추출 (UserDetails 구현 완성 전 임시 처리)
        // 실제 구현 시 CustomUserDetails 캐스팅으로 교체
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            /// CustomUserDetails 완성 후 교체
            // return ((CustomUserDetails) ud).getHouseholdId();
        }
        // 개발 단계 임시 하드코딩 — 반드시 교체 필요
        return 1L;
    }
}