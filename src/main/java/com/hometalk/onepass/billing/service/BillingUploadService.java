package com.hometalk.onepass.billing.service;

import com.hometalk.onepass.billing.entity.Billing;
import com.hometalk.onepass.billing.entity.Billing.BillingStatus;
import com.hometalk.onepass.billing.entity.BillingDetail;
import com.hometalk.onepass.billing.entity.BillingLog;
import com.hometalk.onepass.billing.entity.BillingLog.BillingActionType;
import com.hometalk.onepass.billing.repository.BillingDetailRepository;
import com.hometalk.onepass.billing.repository.BillingLogRepository;
import com.hometalk.onepass.billing.repository.BillingRepository;
import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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

            UpsertType upsertType = UpsertType.ERROR;
            if (!hasError) {
                Optional<Billing> existing = billingRepository
                        .findByHousehold_IdAndBillingMonth(
                                row.getHouseholdId(), row.getBillingMonth());
                upsertType = existing.isPresent() ? UpsertType.UPDATE : UpsertType.INSERT;
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

            Household household = householdRepository.findById(row.getHouseholdId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 세대입니다. id=" + row.getHouseholdId()));

            Optional<Billing> existing = billingRepository
                    .findByHousehold_IdAndBillingMonth(
                            row.getHouseholdId(), row.getBillingMonth());

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

            // billing_details 저장
            List<BillingDetail> details = row.getItems().stream()
                    .map(item -> BillingDetail.builder()
                            .billing(billing)
                            .itemName(item.getItemName())
                            .itemAmount(item.getItemAmount())
                            .build())
                    .toList();
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
        if (row.getHouseholdId() == null) return "세대 정보 누락";
        if (row.getBillingMonth() == null || row.getBillingMonth().isBlank()) return "부과월 누락";
        if (row.getTotalAmount() == null || row.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) return "금액 누락";
        if (row.getDueDate() == null) return "납기일 누락";
        if (row.getItems() == null || row.getItems().isEmpty()) return "상세 항목 누락";
        return null;
    }

    // ─────────────────────────────────────────────
    // 데이터 클래스
    // ─────────────────────────────────────────────

    @Getter
    @Builder
    public static class UploadRow {
        private Long          householdId;
        private String        billingMonth;
        private LocalDate     dueDate;
        private BigDecimal    totalAmount;
        private List<ItemRow> items;
    }

    @Getter
    @Builder
    public static class ItemRow {
        private String     itemName;
        private BigDecimal itemAmount;
    }

    @Getter
    @Builder
    public static class UploadPreviewRow {
        private int        num;
        private Long       householdId;
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