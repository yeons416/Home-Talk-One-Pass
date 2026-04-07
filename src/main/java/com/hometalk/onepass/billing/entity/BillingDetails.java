package com.hometalk.onepass.billing.entity;

import com.hometalk.onepass.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/* 관리비 청구 상세 항목 테이블
 *
 * DB 제약조건:
 *  C2: CHECK(item_amount >= 0)  - 음수 금액 입력 불가
 *  C5: NOT NULL(billing_id, item_name, item_amount)
 *  C6: ON DELETE CASCADE        - 상위 billings 삭제 시 상세 항목 자동 삭제
 *                                 (UPSERT UPDATE 시 Delete-Insert로도 처리)
 *
 * 연관 관계:
 *  Billings (N:1) - billing_id FK, CASCADE DELETE
 */
@Entity
@Table(
        name = "billing_details",
        indexes = {
                @Index(name = "idx_billing_details_billing_id", columnList = "billing_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BillingDetails extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /* FK - 상위 청구 건 (billings.id 참조)
     * - C6: 부모(Billings) 삭제 시 CASCADE로 자동 삭제
     * - UPSERT UPDATE 시: 서비스 레이어에서 billing_id 기준 전체 삭제 후 재삽입
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "billing_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_billing_details_billing_id")
    )
    private Billings billing;

    /* 항목명
     * 예: 일반관리비, 청소비, 전기료, 수도료, 난방비
     * - 고지서 모달 상세 리스트 왼쪽 레이블로 표시
     * - 엑셀 컬럼명과 1:1 매칭하여 파싱
     */
    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    /* 항목별 금액 (C2: CHECK >= 0)
     * - 고지서 모달 상세 리스트 오른쪽 금액으로 표시
     * - Billings.total_amount = 동일 billing_id의 item_amount 합산
     */
    @Column(name = "item_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal itemAmount;
}