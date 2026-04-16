package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.VisitReservation;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class VisitReservationResponse {

    private Long reservationId;
    private String vehicleNumber;
    private String purpose;
    private LocalDateTime reservedAt;
    private String status;
    private String household;

    public VisitReservationResponse(VisitReservation reservation) {
        this.reservationId = reservation.getReservationId();
        this.vehicleNumber = reservation.getVehicleNumber();
        this.purpose = reservation.getPurpose();
        this.reservedAt = reservation.getReservedAt();
        this.status = reservation.getStatus().name();
        this.household = reservation.getHousehold() != null
                ? reservation.getHousehold().getDong() + " " + reservation.getHousehold().getHo()
                : "";
    }
}