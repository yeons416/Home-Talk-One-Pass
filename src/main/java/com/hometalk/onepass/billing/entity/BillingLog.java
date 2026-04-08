package com.hometalk.onepass.billing.entity;

import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "billing_logs",
        indexes = {
                @Index(name = "idx_billing_logs_billing_id",  columnList = "billing_id"),
                @Index(name = "idx_billing_logs_user_id",     columnList = "user_id"),
                @Index(name = "idx_billing_logs_action_type", columnList = "action_type"),
                @Index(name = "idx_billing_logs_created_at",  columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BillingLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "billing_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_billing_logs_billing_id")
    )
    private Billing billing;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingActionType actionType;

    public enum BillingActionType {
        UPLOAD, STATUS_CHANGE
    }
}