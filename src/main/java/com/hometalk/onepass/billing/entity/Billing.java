package com.hometalk.onepass.billing.entity;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "billings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_billings_household_month",
                        columnNames = {"household_id", "billing_month"}
                )
        },
        indexes = {
                @Index(name = "idx_billings_household_id",  columnList = "household_id"),
                @Index(name = "idx_billings_billing_month", columnList = "billing_month"),
                @Index(name = "idx_billings_status",        columnList = "status"),
                @Index(name = "idx_billings_due_date",      columnList = "due_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Billing extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(nullable = false, length = 7)
    private String billingMonth;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private BillingStatus status = BillingStatus.UNPAID;

    public void markAsPaid() {
        this.status = BillingStatus.PAID;
    }

    public void updateByUpload(BigDecimal totalAmount, LocalDate dueDate) {
        this.totalAmount = totalAmount;
        this.dueDate = dueDate;
    }

    public enum BillingStatus {
        UNPAID, PAID
    }
}