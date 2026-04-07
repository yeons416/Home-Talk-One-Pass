package com.hometalk.onepass.billing.service;
/*
* 엑셀 업로드 UPSERT
* */

import com.hometalk.onepass.billing.entity.*;
import com.hometalk.onepass.billing.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingUploadService {

    private final BillingsRepository       billingsRepository;
    private final BillingDetailsRepository billingDetailsRepository;
    private final BillingLogsRepository    billingLogsRepository;

    // 엑셀 필수 컬럼
    private static final String COL_HOUSEHOLD_ID  = "household_id";
    private static final String COL_BILLING_MONTH = "billing_month";
    private static final String COL_TOTAL_AMOUNT  = "total_amount";

    // 항목 컬럼 (순서 유지)
    private static final List<String> ITEM_COLS = List.of(
            "일반관리비", "청소비", "전기료", "수도료", "난방비"
    );

    // ═══════════════════════════════════════════════════════
    // 중복 부과월 확인 (팝업 트리거)
    // ═══════════════════════════════════════════════════════

    /**
     * 해당 부과월 데이터가 이미 존재하는지 확인
     * - 사용처: BillingApiController.checkDuplicateMonth()
     */
    public boolean existsByBillingMonth(String billingMonth) {
        return billingLogsRepository.existsUploadLogByBillingMonth(billingMonth);
    }

    // ═══════════════════════════════════════════════════════
    // 엑셀 업로드 확정 (UPSERT)
    // ═══════════════════════════════════════════════════════

    /**
     * 엑셀 파일 파싱 → 유효성 검사 → BILLINGS UPSERT → BILLING_DETAILS Delete-Insert → 로그 기록
     * - 사용처: BillingApiController.confirmUpload()
     * - 응답: { insertCount, updateCount, errorCount }
     */
    @Transactional
    public Map<String, Integer> processUpload(MultipartFile file, Long adminUserId) {

        // 1. 엑셀 파싱
        List<Map<String, Object>> rows = parseExcel(file);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("업로드된 파일에 데이터가 없습니다.");
        }

        // 2. 부과월 추출 (첫 행 기준)
        String billingMonth = String.valueOf(rows.get(0).get(COL_BILLING_MONTH));

        // 3. 해당 부과월 기존 데이터 일괄 조회 (N+1 방지)
        List<Long> householdIds = rows.stream()
                .map(r -> toLong(r.get(COL_HOUSEHOLD_ID)))
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Billings> existingMap = billingsRepository
                .findByBillingMonthAndHouseholdIdIn(billingMonth, householdIds)
                .stream()
                .collect(Collectors.toMap(Billings::getHouseholdId, b -> b));

        // 4. UPSERT 처리
        int insertCount = 0;
        int updateCount = 0;
        int errorCount  = 0;

        for (Map<String, Object> row : rows) {

            try {
                Long       householdId = toLong(row.get(COL_HOUSEHOLD_ID));
                String     month       = String.valueOf(row.get(COL_BILLING_MONTH));
                BigDecimal totalAmount = toBigDecimal(row.get(COL_TOTAL_AMOUNT));

                // 유효성 검사
                if (householdId == null || month == null || month.isBlank()) {
                    errorCount++;
                    continue;
                }
                if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                    errorCount++;
                    continue;
                }

                // due_date: 해당 월 말일 자동 계산
                LocalDate dueDate = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"))
                        .atEndOfMonth();

                // 항목 파싱
                List<Map.Entry<String, BigDecimal>> items = ITEM_COLS.stream()
                        .filter(col -> row.containsKey(col) && toBigDecimal(row.get(col)) != null)
                        .map(col -> Map.entry(col, toBigDecimal(row.get(col))))
                        .toList();

                if (existingMap.containsKey(householdId)) {
                    // ── UPDATE ───────────────────────────────────
                    Billings existing = existingMap.get(householdId);
                    existing.updateByUpload(totalAmount, dueDate);

                    // 상세항목 Delete-Insert
                    billingDetailsRepository.deleteAllByBillingId(existing.getId());
                    saveDetails(existing, items);

                    updateCount++;

                } else {
                    // ── INSERT ───────────────────────────────────
                    Billings newBilling = Billings.builder()
                            .householdId(householdId)
                            .billingMonth(month)
                            .totalAmount(totalAmount)
                            .dueDate(dueDate)
                            .status(BillingStatus.UNPAID)
                            .build();
                    billingsRepository.save(newBilling);
                    saveDetails(newBilling, items);

                    insertCount++;
                }

            } catch (Exception e) {
                errorCount++;
            }
        }

        // 5. 업로드 로그 기록
        String description = String.format(
                "%s 업로드 완료 — 신규 %d건 · 업데이트 %d건 · 오류 %d건",
                billingMonth, insertCount, updateCount, errorCount
        );
        BillingLogs log = BillingLogs.builder()
                .userId(adminUserId)
                .actionType(BillingActionType.UPLOAD)
                // UPLOAD 로그는 billing_id 없이 기록 (전체 작업 단위)
                // nullable = true 이므로 billing 필드 생략
                .build();
        billingLogsRepository.save(log);

        // TODO: NOTIFICATIONS 미납 알림 일괄 생성 (NotificationService 연동)

        return Map.of(
                "insertCount", insertCount,
                "updateCount", updateCount,
                "errorCount",  errorCount
        );
    }

    // ═══════════════════════════════════════════════════════
    // 내부 유틸
    // ═══════════════════════════════════════════════════════

    /** 엑셀 파싱 → List<Map<컬럼명, 값>> */
    private List<Map<String, Object>> parseExcel(MultipartFile file) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row   header = sheet.getRow(0);
            if (header == null) return result;

            // 헤더 컬럼 인덱스 매핑
            Map<Integer, String> colIndex = new HashMap<>();
            for (Cell cell : header) {
                colIndex.put(cell.getColumnIndex(), cell.getStringCellValue().trim());
            }

            // 데이터 행 파싱
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> rowMap = new HashMap<>();
                for (Map.Entry<Integer, String> entry : colIndex.entrySet()) {
                    Cell cell = row.getCell(entry.getKey());
                    rowMap.put(entry.getValue(), getCellValue(cell));
                }
                result.add(rowMap);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("엑셀 파일을 읽는 중 오류가 발생했습니다.", e);
        }
        return result;
    }

    /** Cell 값을 Object로 반환 */
    private Object getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING  -> cell.getStringCellValue().trim();
            case BOOLEAN -> cell.getBooleanCellValue();
            default      -> null;
        };
    }

    /** BillingDetails 일괄 저장 */
    private void saveDetails(Billings billing, List<Map.Entry<String, BigDecimal>> items) {
        List<BillingDetails> details = items.stream()
                .map(e -> BillingDetails.builder()
                        .billing(billing)
                        .itemName(e.getKey())
                        .itemAmount(e.getValue())
                        .build())
                .toList();
        billingDetailsRepository.saveAll(details);
    }

    /** Object → Long 변환 */
    private Long toLong(Object val) {
        if (val == null) return null;
        try {
            if (val instanceof Double d) return d.longValue();
            return Long.parseLong(String.valueOf(val).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Object → BigDecimal 변환 */
    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        try {
            if (val instanceof Double d) return BigDecimal.valueOf(d);
            return new BigDecimal(String.valueOf(val).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}