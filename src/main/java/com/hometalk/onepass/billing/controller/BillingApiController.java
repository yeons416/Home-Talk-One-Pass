package com.hometalk.onepass.billing.controller;

import com.hometalk.onepass.billing.dto.BillingDetailResponse;
import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.service.BillingService;
import com.hometalk.onepass.billing.service.BillingUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리비 REST API 컨트롤러
 * context-path: /hometop  →  실제 경로: /hometop/api/billing/...
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingApiController {

    private final BillingService       billingService;
    private final BillingUploadService billingUploadService;

    // ─────────────────────────────────────────────────────────────
    // 관리자: 고지서 목록 (업로드 화면 DB 모드)
    //   GET /api/billing/admin/list?year=2026&month=2026-03&dong=101동&size=200&page=0
    //   → Page<BillingSummaryResponse>  (content[].billingId, unit, billingMonth, totalAmount, status)
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/admin/list")
    public ResponseEntity<Page<BillingSummaryResponse>> getAdminList(
            @RequestParam(required = false)           Integer year,
            @RequestParam(required = false)           String  month,
            @RequestParam(required = false)           String  dong,
            @RequestParam(defaultValue = "200")       int     size,
            @RequestParam(defaultValue = "0")         int     page
    ) {
        return ResponseEntity.ok(
                billingService.getAdminBillingList(year, month, dong, size, page)
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 관리자: 미납 세대 목록
    //   GET /api/billing/admin/unpaid?year=2026&month=2026-03&dong=101동&overdueOnly=true
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/admin/unpaid")
    public ResponseEntity<Page<BillingSummaryResponse>> getAdminUnpaid(
            @RequestParam(required = false)           Integer year,
            @RequestParam(required = false)           String  month,
            @RequestParam(required = false)           String  dong,
            @RequestParam(required = false)           Boolean overdueOnly,
            @RequestParam(defaultValue = "20")        int     size,
            @RequestParam(defaultValue = "0")         int     page
    ) {
        return ResponseEntity.ok(
                billingService.getAdminUnpaidList(year, month, dong, overdueOnly, size, page)
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 관리자: 통계 (미납 세대 관리 상단 카드)
    //   GET /api/billing/admin/stats?billingMonth=2026-03
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats(
            @RequestParam String billingMonth
    ) {
        return ResponseEntity.ok(billingService.getAdminStats(billingMonth));
    }

    // ─────────────────────────────────────────────────────────────
    // 관리자: 납부완료 처리
    //   POST /api/billing/admin/{billingId}/pay?adminId=1
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/admin/{billingId}/pay")
    public ResponseEntity<Void> markAsPaid(
            @PathVariable Long billingId,
            @RequestParam(defaultValue = "1") Long adminId   // TODO: CustomUserDetails로 교체
    ) {
        billingService.markAsPaid(billingId, adminId);
        return ResponseEntity.ok().build();
    }

    // ─────────────────────────────────────────────────────────────
    // 업로드: 부과월 중복 확인
    //   GET /api/billing/admin/upload/check?billingMonth=2026-03
    //   → { "exists": true/false }
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/admin/upload/check")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(
            @RequestParam String billingMonth
    ) {
        return ResponseEntity.ok(
                Map.of("exists", billingService.existsByBillingMonth(billingMonth))
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 업로드: 확정 저장
    //   POST /api/billing/admin/upload/confirm?adminId=1
    //   Body: [{ householdId, billingMonth, dueDate, totalAmount, items:[{itemName,itemAmount}] }]
    //   → { insertCount, updateCount }
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/admin/upload/confirm")
    public ResponseEntity<BillingUploadService.UploadConfirmResult> confirmUpload(
            @RequestBody  List<BillingUploadService.UploadRow> rows,
            @RequestParam(defaultValue = "1") Long adminId   // TODO: CustomUserDetails로 교체
    ) {
        return ResponseEntity.ok(billingUploadService.confirmUpload(rows, adminId));
    }

    // ─────────────────────────────────────────────────────────────
    // 고지서 상세 (관리자 미리보기 모달 + 입주민 디지털 고지서 모달 공통)
    //   GET /api/billing/{billingId}/detail
    //   → { billingId, billingMonth, dongHo, dueDate, totalAmount, status, items }
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/{billingId}/detail")
    public ResponseEntity<BillingDetailResponse> getDetail(
            @PathVariable Long billingId
    ) {
        return ResponseEntity.ok(billingService.getBillingDetail(billingId));
    }

    // ─────────────────────────────────────────────────────────────
    // 입주민: 관리비 내역 목록
    //   GET /api/billing/resident/list?householdId=1&year=2026&month=2026-03&status=UNPAID
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/resident/list")
    public ResponseEntity<Page<BillingSummaryResponse>> getResidentList(
            @RequestParam                             Long    householdId,  // TODO: CustomUserDetails로 교체
            @RequestParam(required = false)           Integer year,
            @RequestParam(required = false)           String  month,
            @RequestParam(required = false)           String  status,
            @RequestParam(defaultValue = "12")        int     size,
            @RequestParam(defaultValue = "0")         int     page
    ) {
        return ResponseEntity.ok(
                billingService.getResidentBillingList(householdId, year, month, status, size, page)
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 입주민: 요약 카드 (이번 달 청구액 / 미납 건수 / 최근 납부일)
    //   GET /api/billing/resident/summary?householdId=1
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/resident/summary")
    public ResponseEntity<Map<String, Object>> getResidentSummary(
            @RequestParam Long householdId   // TODO: CustomUserDetails로 교체
    ) {
        return ResponseEntity.ok(billingService.getResidentSummary(householdId));
    }
}