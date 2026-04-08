package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.auth.entity.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // 차량 번호로 조회
    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    // 세대별 차량 목록 조회
    List<Vehicle> findByHousehold(Household household);

    // 차량 번호 뒷자리 4자리로 조회 (퀵서치)
    List<Vehicle> findByVehicleNumberEndingWith(String suffix);

    // 승인 상태별 조회
    List<Vehicle> findByStatus(Vehicle.VehicleStatus status);

    // 차량 번호 중복 확인
    boolean existsByVehicleNumber(String vehicleNumber);
}