package com.hometalk.onepass.billing.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.BillingActionType;
import com.hometalk.onepass.billing.entity.BillingDetail;
import com.hometalk.onepass.billing.entity.BillingLog;
import com.hometalk.onepass.billing.entity.BillingStatus;
import com.hometalk.onepass.billing.repository.BillingDetailRepository;
import com.hometalk.onepass.billing.repository.BillingLogRepository;
import com.hometalk.onepass.billing.repository.BillingRepository;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingUploadService {

    private final BillingRepository       billingRepository;
    private final BillingDetailRepository billingDetailRepository;
    private final BillingLogRepository    billingLogRepository;
    private final HouseholdRepository     householdRepository;

    // ─────────────────────────────────────────────
    // 유효성 검사 + 미리보기
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UploadPreviewResult validateAndPreview(List<UploadRow> rows) {

        List<UploadPreviewRow> previewRows = new ArrayList<>();
        int errorCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            UploadRow row = rows.get(i);
            int num = i + 1;

            String validationError = validate(row);
            boolean hasError = validationError != null;
            if (hasError) errorCount++;

            // householdId("101-101") → Household 조회 → 기존 billing 존재 여부로 UPSERT 판별
            UpsertType upsertType = UpsertType.ERROR;
            if (!hasError) {
                Optional<Household> household = findHousehold(row.getHouseholdId());
                if (household.isPresent()) {
                    Optional<Billing> existing = billingRepository
                            .findByHousehold_IdAndBillingMonth(
                                    household.get().getId(), row.getBillingMonth());
                    upsertType = existing.isPresent() ? UpsertType.UPDATE : UpsertType.INSERT;
                } else {
                    upsertType = UpsertType.INSERT;
                }
            }

            previewRows.add(UploadPreviewRow.builder()
                    .num(num)
                    .householdId(row.getHouseholdId())
                    .billingMonth(row.getBillingMonth())
                    .totalAmount(row.getTotalAmount())
                    .validationError(validationError)
                    .upsertType(upsertType)
                    .build());
        }

        return new UploadPreviewResult(rows.size(), errorCount, previewRows);
    }

    // ─────────────────────────────────────────────
    // 업로드 확정 (UPSERT)
    // ─────────────────────────────────────────────

    @Transactional
    public UploadConfirmResult confirmUpload(List<UploadRow> rows, Long adminId) {

        int insertCount = 0;
        int updateCount = 0;

        for (UploadRow row : rows) {

            if (validate(row) != null) continue;

            // householdId("101-101") → Household 조회
            Optional<Household> householdOpt = findHousehold(row.getHouseholdId());
            if (householdOpt.isEmpty()) continue; // 세대 없으면 스킵

            Household household = householdOpt.get();

            Optional<Billing> existing = billingRepository
                    .findByHousehold_IdAndBillingMonth(
                            household.getId(), row.getBillingMonth());

            Billing billing;

            if (existing.isPresent()) {
                // UPDATE
                billing = existing.get();
                billing.updateByUpload(row.getTotalAmount(), row.getDueDate());
                billingDetailRepository.deleteByBilling_Id(billing.getId());
                updateCount++;
            } else {
                // INSERT
                billing = billingRepository.save(Billing.builder()
                        .household(household)
                        .billingMonth(row.getBillingMonth())
                        .dueDate(row.getDueDate())
                        .totalAmount(row.getTotalAmount())
                        .status(BillingStatus.UNPAID)
                        .build());
                insertCount++;
            }

            // billing_details 저장 (sortOrder 포함)
            List<BillingDetail> details = new ArrayList<>();
            List<ItemRow> items = row.getItems();
            for (int i = 0; i < items.size(); i++) {
                details.add(BillingDetail.builder()
                        .billing(billing)
                        .itemName(items.get(i).getItemName())
                        .itemAmount(items.get(i).getItemAmount())
                        .sortOrder(i)
                        .build());
            }
            billingDetailRepository.saveAll(details);

            // billing_logs UPLOAD 기록
            billingLogRepository.save(BillingLog.builder()
                    .billing(billing)
                    .userId(adminId)
                    .actionType(BillingActionType.UPLOAD)
                    .build());
        }

        return new UploadConfirmResult(insertCount, updateCount);
    }

    // ─────────────────────────────────────────────
    // 유효성 검사
    // ─────────────────────────────────────────────

    private String validate(UploadRow row) {
        if (row.getHouseholdId() == null || row.getHouseholdId().isBlank()) return "세대 정보 누락";
        if (row.getBillingMonth() == null || row.getBillingMonth().isBlank()) return "부과월 누락";
        if (row.getTotalAmount() == null || row.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) return "금액 누락";
        if (row.getDueDate() == null) return "납기일 누락";
        if (row.getItems() == null || row.getItems().isEmpty()) return "상세 항목 누락";
        return null;
    }

    // ─────────────────────────────────────────────
    // 내부 유틸
    // ─────────────────────────────────────────────

    /**
     * householdId("101-101") → dong="101동", ho="101호" 변환 후 Household 조회
     * 엑셀 동/호 형식: "동번호-호번호" (예: "101-101", "102-305")
     */
    private Optional<Household> findHousehold(String householdId) {
        String[] parts = householdId.split("-");
        if (parts.length < 2) return Optional.empty();
        String dong = parts[0] + "동";  // "101" → "101동"
        String ho   = parts[1] + "호";  // "101" → "101호"
        return householdRepository.findByDongAndHo(dong, ho);
    }

    // ─────────────────────────────────────────────
    // 데이터 클래스
    // ─────────────────────────────────────────────

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadRow {
        private String        householdId;  // 엑셀 동/호 값 (예: "101-101")
        private String        billingMonth;
        private LocalDate     dueDate;
        private BigDecimal    totalAmount;
        private List<ItemRow> items;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRow {
        private String     itemName;
        private BigDecimal itemAmount;
    }

    @Getter
    @Builder
    public static class UploadPreviewRow {
        private int        num;
        private String     householdId;
        private String     billingMonth;
        private BigDecimal totalAmount;
        private String     validationError;
        private UpsertType upsertType;
    }

    public record UploadPreviewResult(
            int totalCount,
            int errorCount,
            List<UploadPreviewRow> rows
    ) {}

    public record UploadConfirmResult(
            int insertCount,
            int updateCount
    ) {}

    public enum UpsertType {
        INSERT, UPDATE, ERROR
    }
}