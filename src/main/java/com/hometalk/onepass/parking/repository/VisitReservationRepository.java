package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.parking.entity.VisitReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitReservationRepository extends JpaRepository<VisitReservation, Long> {

    // 세대별 예약 목록 조회
    List<VisitReservation> findByHousehold(Household household);

    // 세대별 예약 목록 조회 (householdId 기반)
    List<VisitReservation> findByHousehold_Id(Long householdId);

    // 세대별 예약 상태별 조회
    List<VisitReservation> findByHouseholdAndStatus(Household household, VisitReservation.ReservationStatus status);

    // 세대별 예약 상태별 조회 (householdId 기반)
    List<VisitReservation> findByHousehold_IdAndStatus(Long householdId, VisitReservation.ReservationStatus status);

    // 차량 번호 뒷자리 4자리로 조회 (퀵서치)
    List<VisitReservation> findByVehicleNumberEndingWithAndStatus(String suffix, VisitReservation.ReservationStatus status);

    // 당일 예약 목록 조회
    List<VisitReservation> findByReservedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, VisitReservation.ReservationStatus status);

    // 중복 예약 확인
    boolean existsByVehicleNumberAndReservedAt(String vehicleNumber, LocalDateTime reservedAt);
}