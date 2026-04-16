package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.parking.entity.ParkingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingLogRepository extends JpaRepository<ParkingLog, Long> {

    // 세대별 주차 기록 조회
    List<ParkingLog> findByHousehold(Household household);

    // 세대별 월별 주차 기록 조회
    List<ParkingLog> findByHouseholdAndEntryTimeBetween(
            Household household,
            java.time.LocalDateTime start,
            java.time.LocalDateTime end
    );

    // 차량 번호 뒷자리 4자리로 조회 (퀵서치)
    List<ParkingLog> findByVehicleNumberEndingWithAndStatus(
            String suffix,
            ParkingLog.ParkingStatus status
    );

    // 현재 주차 중인 차량 조회
    List<ParkingLog> findByStatus(ParkingLog.ParkingStatus status);

    // 전체 월별 주차 기록 조회 (관리자)
    List<ParkingLog> findByEntryTimeBetween(
            java.time.LocalDateTime start,
            java.time.LocalDateTime end
    );

    // 미등록 차량 조회 (세대 미연결)
    List<ParkingLog> findByHouseholdIsNullAndStatus(ParkingLog.ParkingStatus status);

    // 차량 번호로 현재 주차 중인 기록 조회
    java.util.Optional<ParkingLog> findByVehicleNumberAndStatus(
            String vehicleNumber,
            ParkingLog.ParkingStatus status
    );
}