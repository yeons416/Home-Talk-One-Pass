package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.ParkingLog;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ParkingLogResponse {

    private Long parkingId;
    private String vehicleNumber;
    private String household;
    private String entryType;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Integer totalMinutes;
    private Integer appliedMinutes;
    private String status;
    private String vehicleType;
    private String settlement;

    public ParkingLogResponse(ParkingLog log) {
        this.parkingId = log.getParkingId();
        this.vehicleNumber = log.getVehicleNumber();
        this.household = log.getHousehold() != null
                ? log.getHousehold().getDong() + " " + log.getHousehold().getHo()
                : "-";
        this.entryType = log.getEntryType().name();
        this.entryTime = log.getEntryTime();
        this.exitTime = log.getExitTime();
        this.totalMinutes = log.getTotalMinutes();
        this.appliedMinutes = log.getAppliedMinutes();
        this.status = log.getStatus().name();
        this.vehicleType = log.getEntryType() == ParkingLog.EntryType.NORMAL ? "입주자 차량" : "방문 차량";
        this.settlement = log.getAppliedMinutes() != null && log.getTotalMinutes() != null
                ? (log.getTotalMinutes() <= log.getAppliedMinutes() ? "불필요" : "완료")
                : "-";
    }
}