package com.hometalk.onepass.billing.controller;

import com.hometalk.onepass.billing.dto.BillingDetailResponse;
import com.hometalk.onepass.billing.dto.BillingSummaryResponse;
import com.hometalk.onepass.billing.dto.ResidentBillingResponse;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.billing.service.BillingService;
import com.hometalk.onepass.billing.service.BillingUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingApiController {

    private final BillingService       billingService;
    private final BillingUploadService billingUploadService;

    // ═══════════════════════════════════════════════════════
    // 입주민 API
    // ═══════════════════════════════════════════════════════

    /**
     * 입주민 관리비 목록 필터 재조회
     * GET /hometop/api/billing?year=2026&month=03&status=UNPAID
     *
     * 사용처: 입주민 페이지 필터 변경 시 JS fetch
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RESIDENT','ADMIN')")
    public ResponseEntity<List<BillingSummaryResponse>> getBillingList(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) BillingStatus status
    ) {
        Long householdId = getHouseholdId();
        List<BillingSummaryResponse> list =
                billingService.getBillingList(householdId, year, month, status);
        return ResponseEntity.ok(list);
    }

    /**
     * 고지서 모달 상세 조회
     * GET /hometop/api/billing/{billingId}/detail
     *
     * 사용처:
     *  - 입주민 "고지서 보기" 클릭 (mode: resident)
     *  - 관리자 업로드 후 "미리보기 →" 클릭 (mode: preview, billingId 확정 후)
     */
    @GetMapping("/{billingId}/detail")
    @PreAuthorize("hasAnyRole('RESIDENT','ADMIN')")
    public ResponseEntity<BillingDetailResponse> getBillingDetail(
            @PathVariable Long billingId
    ) {
        Long householdId = getHouseholdId();
        BillingDetailResponse detail =
                billingService.getBillingDetail(billingId, householdId);
        return ResponseEntity.ok(detail);
    }

    // ═══════════════════════════════════════════════════════
    // 관리자 API — 고지서 업로드
    // ═══════════════════════════════════════════════════════

    /**
     * 중복 부과월 확인
     * GET /hometop/api/billing/admin/upload/check?billingMonth=2026-03
     *
     * 사용처: 엑셀 파일 선택 시 중복 확인 팝업 트리거 여부 판별
     * 응답: { "exists": true/false }
     */
    @GetMapping("/admin/upload/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> checkDuplicateMonth(
            @RequestParam String billingMonth
    ) {
        boolean exists = billingUploadService.existsByBillingMonth(billingMonth);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * 엑셀 업로드 확정
     * POST /hometop/api/billing/admin/upload
     *
     * 사용처: 관리자 "업로드 확정 ↑" 버튼 클릭
     * 처리:
     *  - BILLINGS 테이블 UPSERT (INSERT or UPDATE)
     *  - BILLING_DETAILS Delete-Insert
     *  - BILLING_LOGS UPLOAD 기록
     *  - NOTIFICATIONS 미납 알림 생성
     * 응답: { "insertCount": 85, "updateCount": 34, "errorCount": 1 }
     */
    @PostMapping("/admin/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> confirmUpload(
            @RequestParam("file") MultipartFile file
    ) {
        Long adminUserId = getUserId();
        Map<String, Integer> result = billingUploadService.processUpload(file, adminUserId);
        return ResponseEntity.ok(result);
    }

    // ═══════════════════════════════════════════════════════
    // 관리자 API — 미납 세대 관리
    // ═══════════════════════════════════════════════════════

    /**
     * 납부완료 처리
     * PATCH /hometop/api/billing/admin/{billingId}/pay
     *
     * 사용처: 관리자 "납부완료 처리" confirm 클릭
     * 처리:
     *  - billings.status UNPAID → PAID
     *  - BILLING_LOGS STATUS_CHANGE 기록
     *  - 입주민 납부 완료 알림 생성
     * 응답: 200 OK
     */
    @PatchMapping("/admin/{billingId}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAsPaid(
            @PathVariable Long billingId
    ) {
        Long adminUserId = getUserId();
        billingService.markAsPaid(billingId, adminUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * 관리자 미납 목록 필터 재조회 (JS fetch용)
     * GET /hometop/api/billing/admin/unpaid?year=2026&month=03&dong=101&filter=unpaid&page=0
     *
     * 사용처: 관리자 미납 세대 필터 변경 시 JS fetch → tbody 교체
     */
    @GetMapping("/admin/unpaid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminUnpaidList(
            @RequestParam(required = false)            String year,
            @RequestParam(required = false)            String month,
            @RequestParam(required = false)            String dong,
            @RequestParam(required = false)            String filter,
            @RequestParam(defaultValue = "0")          int    page,
            @RequestParam(defaultValue = "10")         int    size
    ) {
        BillingStatus statusFilter = switch (filter != null ? filter : "") {
            case "unpaid" -> BillingStatus.UNPAID;
            case "paid"   -> BillingStatus.PAID;
            default       -> null;
        };
        boolean longOverdue = "long".equals(filter);

        org.springframework.data.domain.PageRequest pageable =
                org.springframework.data.domain.PageRequest.of(page, size);

        org.springframework.data.domain.Page<BillingSummaryResponse> result = longOverdue
                ? billingService.getLongOverdueList(pageable)
                : billingService.getAdminUnpaidList(statusFilter, year, month, dong, pageable);

        return ResponseEntity.ok(Map.of(
                "content",      result.getContent(),
                "currentPage",  result.getNumber(),
                "totalPages",   result.getTotalPages(),
                "totalElements",result.getTotalElements()
        ));
    }

    // ═══════════════════════════════════════════════════════
    // 내부 유틸
    // ═══════════════════════════════════════════════════════

    /**
     * 로그인 사용자 householdId 추출
     * TODO: CustomUserDetails 완성 후 교체
     */
    private Long getHouseholdId() {
        // TODO: return ((CustomUserDetails) auth.getPrincipal()).getHouseholdId();
        return 1L;
    }

    /**
     * 로그인 사용자 userId 추출
     * TODO: CustomUserDetails 완성 후 교체
     */
    private Long getUserId() {
        // TODO: return ((CustomUserDetails) auth.getPrincipal()).getId();
        return 1L;
    }
}