package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.parking.entity.VehicleApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleApprovalRepository extends JpaRepository<VehicleApproval, Long> {

    // 차량별 최신 승인 이력 조회
    Optional<VehicleApproval> findTopByVehicleOrderByApprovalIdDesc(Vehicle vehicle);

    // 승인 상태별 조회
    List<VehicleApproval> findByStatus(VehicleApproval.ApprovalStatus status);

    // 차량별 전체 승인 이력 조회
    List<VehicleApproval> findByVehicleOrderByApprovalIdDesc(Vehicle vehicle);
}