package com.hometalk.onepass.parking.entity;


import com.hometalk.onepass.entity.BaseTimeEntity;
import com.hometalk.onepass.entity.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_approvals")
@Getter
@NoArgsConstructor
public class VehicleApproval extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long approvalId;

    // 승인 대상 차량
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    // 승인/반려 처리한 관리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    // 증빙 서류 경로
    @Column(name = "document_path", nullable = false, length = 500)
    private String documentPath;

    // 승인 상태 (처리 결과만 관리)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status;

    // 반려 사유
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    // 처리 일시
    @Column(name = "processed_at")
    private LocalDateTime processedAt;


    // 생성자 (초기 상태: 승인 대기)
    public VehicleApproval(Vehicle vehicle, String documentPath) {
        if (vehicle == null) {
            throw new IllegalArgumentException("차량 정보는 필수입니다.");
        }
        if (documentPath == null || documentPath.isBlank()) {
            throw new IllegalArgumentException("증빙 서류는 필수입니다.");
        }

        this.vehicle = vehicle;
        this.documentPath = documentPath;
        this.status = ApprovalStatus.PENDING;
    }

    // 승인 처리
    public void approve(User admin) {
        if (this.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        this.status = ApprovalStatus.APPROVED;
        this.admin = admin;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(User admin, String reason) {
        if (this.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        this.status = ApprovalStatus.REJECTED;
        this.admin = admin;
        this.rejectReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
}