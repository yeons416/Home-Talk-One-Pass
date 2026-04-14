package com.hometalk.onepass.parking.entity;

import com.hometalk.onepass.common.entity.BaseSoftDeleteEntity;
import com.hometalk.onepass.auth.entity.Household;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "visit_reservations")
@Getter
@NoArgsConstructor
public class VisitReservation extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "vehicle_number", nullable = false, length = 20)
    private String vehicleNumber;

    @Column(nullable = false, length = 100)
    private String purpose;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    public VisitReservation(Household household, String vehicleNumber, String purpose, LocalDateTime reservedAt) {
        if (household == null) {
            throw new IllegalArgumentException("세대 정보는 필수입니다.");
        }
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new IllegalArgumentException("차량 번호는 필수입니다.");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("방문 목적은 필수입니다.");
        }
        if (reservedAt == null) {
            throw new IllegalArgumentException("방문 예정 시간은 필수입니다.");
        }
        if (reservedAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("방문 예정 시간은 현재 이후여야 합니다.");
        }
        this.household = household;
        this.vehicleNumber = vehicleNumber;
        this.purpose = purpose;
        this.reservedAt = reservedAt;
        this.status = ReservationStatus.RESERVED;
    }

    public void update(String vehicleNumber, String purpose, LocalDateTime reservedAt) {
        if (this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("예약 상태에서만 수정할 수 있습니다.");
        }
        if (vehicleNumber != null && !vehicleNumber.isBlank()) {
            this.vehicleNumber = vehicleNumber;
        }
        if (purpose != null && !purpose.isBlank()) {
            this.purpose = purpose;
        }
        if (reservedAt != null) {
            if (reservedAt.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("방문 예정 시간은 현재 이후여야 합니다.");
            }
            this.reservedAt = reservedAt;
        }
    }

    public void cancel() {
        if (this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("예약 상태에서만 취소할 수 있습니다.");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void enter() {
        if (this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("예약 상태에서만 입차 처리할 수 있습니다.");
        }
        this.status = ReservationStatus.ENTERED;
    }

    public enum ReservationStatus {
        RESERVED, ENTERED, CANCELLED
    }
}