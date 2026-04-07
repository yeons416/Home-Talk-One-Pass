package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.parking.entity.Payment;
import com.hometalk.onepass.parking.entity.ParkingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 주차 기록별 결제 내역 조회
    List<Payment> findByParkingLog(ParkingLog parkingLog);

    // 주차 기록별 결제 상태별 조회
    Optional<Payment> findByParkingLogAndStatus(ParkingLog parkingLog, Payment.PaymentStatus status);

    // 결제 상태별 조회
    List<Payment> findByStatus(Payment.PaymentStatus status);
}