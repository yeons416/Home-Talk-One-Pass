package com.hometalk.onepass.entity.dashboard;

import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class BillingUser {

    private Long userId;
    private STATUS status;
    private String billingMonth;
    private String[] itemName;
    private int[] itemAmount;
    private int totalAmount;
    private LocalDateTime dueDate;

    @Builder
    public BillingUser(Long userId, STATUS status, String billingMonth, String[] itemName, int[] itemAmount,
                       int totalAmount, LocalDateTime dueDate) {
        this.userId = userId;
        this.status = status;
        this.billingMonth = billingMonth;
        this.itemName = itemName;
        this.itemAmount = itemAmount;
        this.totalAmount = totalAmount;
        this.dueDate = dueDate;
    }
}
