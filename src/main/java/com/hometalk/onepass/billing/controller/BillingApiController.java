package com.hometalk.onepass.billing.controller;

import com.hometalk.onepass.billing.dto.BillingDetailResponse;
import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.dto.ResidentBillingResponse;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.billing.service.BillingService;
import com.hometalk.onepass.billing.service.BillingService.AdminBillingStats;
import com.hometalk.onepass.billing.service.BillingUploadService;
import com.hometalk.onepass.billing.service.BillingUploadService.UploadConfirmResult;
import com.hometalk.onepass.billing.service.BillingUploadService.UploadPreviewResult;
import com.hometalk.onepass.billing.service.BillingUploadService.UploadRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingApiController {

    private final BillingService       billingService;
    private final BillingUploadService billingUploadService;

    /**
     * 입주민 관리비 초기 데이터
     * GET /api/billing?householdId=1
     */
    @GetMapping
    public ResponseEntity<ResidentBillingResponse> getResidentBillingPage(
            @RequestParam Long householdId  // TODO: @AuthenticationPrincipal로 교체
    ) {
        return ResponseEntity.ok(billingService.getResidentBillingPage(householdId));
    }

    /**
     * 입주민 관리비 목록 필터 조회
     * GET /api/billing/list?householdId=1&year=2026&month=2026-02&status=UNPAID
     */
    @GetMapping("/list")
    public ResponseEntity<Page<BillingSummaryResponse>> getBillingList(
            @RequestParam Long householdId,  // TODO: @AuthenticationPrincipal로 교체
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) BillingStatus status,
            @PageableDefault(size = 3, sort = "billingMonth", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                billingService.getBillingList(householdId, year, month, status, pageable));
    }

    /**
     * 고지서 모달 상세 조회
     * GET /api/billing/{billingId}/detail
     */
    @GetMapping("/{billingId}/detail")
    public ResponseEntity<BillingDetailResponse> getBillingDetail(
            @PathVariable Long billingId
    ) {
        return ResponseEntity.ok(billingService.getBillingDetail(billingId));
    }

    /**
     * 관리자 통계
     * GET /api/billing/admin/stats?billingMonth=2026-03
     */
    @GetMapping("/admin/stats")
    public ResponseEntity<AdminBillingStats> getStats(
            @RequestParam String billingMonth
    ) {
        return ResponseEntity.ok(billingService.getAdminStats(billingMonth));
    }

    /**
     * 관리자 미납 세대 목록
     * GET /api/billing/admin/unpaid
     */
    @GetMapping("/admin/unpaid")
    public ResponseEntity<Page<BillingSummaryResponse>> getUnpaidList(
            @RequestParam(required = false) String dong,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) BillingStatus status,
            @RequestParam(defaultValue = "false") boolean overdue,
            @PageableDefault(size = 20, sort = "billingMonth", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                billingService.getUnpaidList(dong, year, month, status, overdue, pageable));
    }

    /**
     * 납부완료 처리
     * PATCH /api/billing/admin/{billingId}/paid
     */
    @PatchMapping("/admin/{billingId}/paid")
    public ResponseEntity<Void> markAsPaid(
            @PathVariable Long billingId,
            @RequestParam Long adminId  // TODO: @AuthenticationPrincipal로 교체
    ) {
        billingService.markAsPaid(billingId, adminId);
        return ResponseEntity.ok().build();
    }

    /**
     * 엑셀 업로드 미리보기
     * POST /api/billing/admin/upload/preview
     */
    @PostMapping("/admin/upload/preview")
    public ResponseEntity<UploadPreviewResult> preview(
            @RequestBody List<UploadRow> rows
    ) {
        return ResponseEntity.ok(billingUploadService.validateAndPreview(rows));
    }

    /**
     * 부과월 중복 확인
     * GET /api/billing/admin/upload/check?billingMonth=2026-03
     */
    @GetMapping("/admin/upload/check")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(
            @RequestParam String billingMonth
    ) {
        boolean exists = billingService.existsByBillingMonth(billingMonth);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * 업로드 확정
     * POST /api/billing/admin/upload/confirm
     */
    @PostMapping("/admin/upload/confirm")
    public ResponseEntity<UploadConfirmResult> confirm(
            @RequestBody List<UploadRow> rows,
            @RequestParam Long adminId  // TODO: @AuthenticationPrincipal로 교체
    ) {
        return ResponseEntity.ok(billingUploadService.confirmUpload(rows, adminId));
    }
    /**
     * 관리자 고지서 전체 목록 조회
     * GET /api/billing/admin/list?year=2026&month=2026-03&dong=101동
     */
    @GetMapping("/admin/list")
    public ResponseEntity<Page<BillingSummaryResponse>> getAdminList(
            @RequestParam(required = false) String dong,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String month,
            @PageableDefault(size = 50, sort = "billingMonth", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                billingService.getAdminList(dong, year, month, pageable));
    }
}