package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.ParkingLog;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class ParkingLogResponse {

    private final Long parkingId;
    private final String vehicleNumber;
    private final String household;
    private final String entryType;
    private final String entryTime;
    private final String status;
    private final String purpose;
    private final String reservedAt;
    private final String userName;
    private final String parkingTime;
    private final String ticketInfo;
    private final boolean canExit;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MM.dd HH:mm");

    public ParkingLogResponse(ParkingLog log, int availableMinutes) {
        this.parkingId = log.getParkingId();
        this.vehicleNumber = log.getVehicleNumber();
        this.household = log.getHousehold() != null
                ? log.getHousehold().getDong() + " " + log.getHousehold().getHo()
                : "세대 미확인";
        this.entryType = log.getEntryType().name();
        this.entryTime = log.getEntryTime().format(FMT);
        this.status = log.getStatus().name();
        this.purpose = log.getReservation() != null
                ? log.getReservation().getPurpose()
                : null;
        this.reservedAt = log.getReservation() != null
                && log.getReservation().getReservedAt() != null
                ? log.getReservation().getReservedAt().format(FMT)
                : null;
        this.userName = log.getVehicle() != null
                && log.getVehicle().getUser() != null
                ? log.getVehicle().getUser().getName()
                : null;

        // 주차 시간 계산
        long totalMinutes = Duration.between(
                log.getEntryTime(), LocalDateTime.now()).toMinutes();
        this.parkingTime = formatMinutes(totalMinutes);

        // 티켓 정보
        this.ticketInfo = availableMinutes > 0
                ? formatMinutes(availableMinutes) + " 사용 가능"
                : "티켓 없음";

        // 출차 가능 여부 (티켓으로 전부 커버 가능하면 출차 가능)
        this.canExit = availableMinutes >= totalMinutes;
    }

    // 기존 생성자 (티켓 정보 없을 때)
    public ParkingLogResponse(ParkingLog log) {
        this(log, 0);
    }

    private String formatMinutes(long minutes) {
        if (minutes >= 60) {
            return (minutes / 60) + "시간 " + (minutes % 60) + "분";
        }
        return minutes + "분";
    }
}