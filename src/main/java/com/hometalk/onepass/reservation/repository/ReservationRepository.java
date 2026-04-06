package com.hometalk.onepass.reservation.repository;

import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 1. [수정] 본인이 이 시설을 이미 예약했는지 체크
    // 엔티티의 필드명이 user라면 UserId로 작성하면 JPA가 알아서 user.id를 찾습니다.
    boolean existsByFacilityIdAndUserId(Long facilityId, Long userId);

    // 2. 해당 시간에 이미 다른 예약이 있는지 체크 (기존 유지)
    boolean existsByFacilityIdAndReservationTime(Long facilityId, ReservationTime reservationTime);
}