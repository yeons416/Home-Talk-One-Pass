package com.hometalk.onepass.reservation.dto;

import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class ReservationResponseDto {
    private Long id;
    private String facilityName;
    private String userName;        // [변경] ID 대신 사용자 이름을 보여주면 더 친절하겠죠?
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;

    public static ReservationResponseDto fromEntity(Reservation reservation) {
        ReservationResponseDto dto = new ReservationResponseDto();
        dto.setId(reservation.getId());
        dto.setFacilityName(reservation.getFacility().getName());

        // [수정 예정] reservation.getMemberId() -> reservation.getUser().getName()
        // 지금은 User가 없으니 일단 주석이나 기존 필드로 유지하세요!
        // dto.setUserName(reservation.getUser().getName());

        dto.setStartTime(reservation.getReservationTime().getStartTime());
        dto.setEndTime(reservation.getReservationTime().getEndTime());
        dto.setStatus(reservation.getStatus());
        return dto;
    }
}
