package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.parking.dto.response.ParkingLogResponse;
import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.ParkingTicket;
import com.hometalk.onepass.parking.entity.TicketUsage;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import com.hometalk.onepass.parking.repository.ParkingTicketRepository;
import com.hometalk.onepass.parking.repository.TicketUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffExitService {

    private final ParkingLogRepository parkingLogRepository;
    private final ParkingTicketRepository parkingTicketRepository;
    private final TicketUsageRepository ticketUsageRepository;

    // ─── 출차 차량 퀵서치 ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ParkingLogResponse> search(String keyword) {
        String last4 = keyword.replace(" ", "");

        if (last4.length() != 4) return List.of();

        return parkingLogRepository.findParkedByLast4(last4)
                .stream()
                .map(parkingLog -> {
                    int availableMinutes = getAvailableMinutes(parkingLog);
                    return new ParkingLogResponse(parkingLog, availableMinutes);
                })
                .toList();
    }

    // ─── 주차 중인 방문 차량 목록 ────────────────────────────────
    @Transactional(readOnly = true)
    public List<ParkingLogResponse> getParkedVisitList() {
        return parkingLogRepository.findByStatus(ParkingLog.ParkingStatus.PARKED)
                .stream()
                .filter(parkingLog -> parkingLog.getEntryType() == ParkingLog.EntryType.RESERVATION
                        || parkingLog.getEntryType() == ParkingLog.EntryType.MANUAL)
                .map(parkingLog -> {
                    int availableMinutes = getAvailableMinutes(parkingLog);
                    return new ParkingLogResponse(parkingLog, availableMinutes);
                })
                .toList();
    }

    // ─── 주차 중인 입주자 차량 목록 ──────────────────────────────
    @Transactional(readOnly = true)
    public List<ParkingLogResponse> getParkedResidentList() {
        return parkingLogRepository.findByStatus(ParkingLog.ParkingStatus.PARKED)
                .stream()
                .filter(parkingLog -> parkingLog.getEntryType() == ParkingLog.EntryType.NORMAL)
                .map(parkingLog -> {
                    int availableMinutes = getAvailableMinutes(parkingLog);
                    return new ParkingLogResponse(parkingLog, availableMinutes);
                })
                .toList();
    }

    // ─── 출차 처리 ───────────────────────────────────────────────
    @Transactional
    public void processExit(Long parkingId) {
        ParkingLog parkingLog = parkingLogRepository.findByIdWithLock(parkingId)
                .orElseThrow(() -> new IllegalArgumentException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getStatus() != ParkingLog.ParkingStatus.PARKED) {
            throw new IllegalStateException("이미 출차된 차량입니다.");
        }

        int totalMinutes = (int) Duration.between(
                parkingLog.getEntryTime(), LocalDateTime.now()).toMinutes();
        int availableMinutes = getAvailableMinutes(parkingLog);

        // 티켓 부족 시 출차 불가
        if (availableMinutes < totalMinutes) {
            throw new IllegalStateException("티켓이 부족합니다. 현장 결제 후 강제 출차 처리해주세요.");
        }

        int appliedMinutes = 0;
        if (parkingLog.getHousehold() != null) {
            appliedMinutes = applyTickets(parkingLog, totalMinutes);
        }

        parkingLog.exit(totalMinutes, appliedMinutes);
        // TODO: 알림 - "출차 완료"
    }

    // ─── 강제 출차 처리 (현장 결제 완료 후) ─────────────────────
    @Transactional
    public void forceExit(Long parkingId) {
        ParkingLog parkingLog = parkingLogRepository.findByIdWithLock(parkingId)
                .orElseThrow(() -> new IllegalArgumentException("주차 기록을 찾을 수 없습니다."));

        if (parkingLog.getStatus() != ParkingLog.ParkingStatus.PARKED) {
            throw new IllegalStateException("이미 출차된 차량입니다.");
        }

        int totalMinutes = (int) Duration.between(
                parkingLog.getEntryTime(), LocalDateTime.now()).toMinutes();

        int appliedMinutes = 0;
        if (parkingLog.getHousehold() != null) {
            appliedMinutes = applyTickets(parkingLog, totalMinutes);
        }

        // 현장 결제 완료로 강제 출차
        parkingLog.exit(totalMinutes, appliedMinutes);
        log.info("강제 출차 처리 - parkingId: {}, 현장 결제 완료", parkingId);
        // TODO: 알림 - "출차 완료"
    }

    // ─── 사용 가능한 티켓 시간 계산 ─────────────────────────────
    private int getAvailableMinutes(ParkingLog parkingLog) {
        if (parkingLog.getHousehold() == null) return 0;

        Household household = parkingLog.getHousehold();
        LocalDate today = LocalDate.now();
        int availableMinutes = 0;

        // DAY권
        Optional<ParkingTicket> dayTicketOpt = parkingTicketRepository
                .findByHouseholdAndTypeAndIssueYearAndIssueMonth(
                        household, ParkingTicket.TicketType.DAY,
                        today.getYear(), today.getMonthValue());
        if (dayTicketOpt.isPresent()) {
            availableMinutes += dayTicketOpt.get().getRemainingCount()
                    * ParkingTicket.TicketType.DAY.toMinutes(1);
        }

        // HOUR권
        Optional<ParkingTicket> hourTicketOpt = parkingTicketRepository
                .findByHouseholdAndTypeAndIssueYearAndIssueMonth(
                        household, ParkingTicket.TicketType.HOUR,
                        today.getYear(), today.getMonthValue());
        if (hourTicketOpt.isPresent()) {
            availableMinutes += hourTicketOpt.get().getRemainingCount()
                    * ParkingTicket.TicketType.HOUR.toMinutes(1);
        }

        return availableMinutes;
    }

    // ─── 티켓 적용 로직 ──────────────────────────────────────────
    private int applyTickets(ParkingLog parkingLog, int totalMinutes) {
        Household household = parkingLog.getHousehold();
        LocalDate today = LocalDate.now();
        int remainingMinutes = totalMinutes;
        int appliedMinutes = 0;

        // 1. DAY권 먼저 적용
        Optional<ParkingTicket> dayTicketOpt = parkingTicketRepository
                .findByHouseholdAndTypeAndIssueYearAndIssueMonth(
                        household, ParkingTicket.TicketType.DAY,
                        today.getYear(), today.getMonthValue());

        if (dayTicketOpt.isPresent()) {
            ParkingTicket dayTicket = dayTicketOpt.get();
            int dayMinutes = ParkingTicket.TicketType.DAY.toMinutes(1);
            int usableCount = Math.min(
                    dayTicket.getRemainingCount(),
                    remainingMinutes / dayMinutes
            );

            if (usableCount > 0) {
                TicketUsage usage = new TicketUsage(parkingLog, dayTicket, usableCount);
                ticketUsageRepository.save(usage);
                int used = usableCount * dayMinutes;
                appliedMinutes += used;
                remainingMinutes -= used;
                log.info("DAY권 {}장 사용 - {}분 적용", usableCount, used);
            }
        }

        // 2. HOUR권 적용
        if (remainingMinutes > 0) {
            Optional<ParkingTicket> hourTicketOpt = parkingTicketRepository
                    .findByHouseholdAndTypeAndIssueYearAndIssueMonth(
                            household, ParkingTicket.TicketType.HOUR,
                            today.getYear(), today.getMonthValue());

            if (hourTicketOpt.isPresent()) {
                ParkingTicket hourTicket = hourTicketOpt.get();
                int hourMinutes = ParkingTicket.TicketType.HOUR.toMinutes(1);
                int usableCount = Math.min(
                        hourTicket.getRemainingCount(),
                        (int) Math.ceil((double) remainingMinutes / hourMinutes)
                );

                if (usableCount > 0) {
                    TicketUsage usage = new TicketUsage(parkingLog, hourTicket, usableCount);
                    ticketUsageRepository.save(usage);
                    int used = usableCount * hourMinutes;
                    appliedMinutes += used;
                    log.info("HOUR권 {}장 사용 - {}분 적용", usableCount, used);
                }
            }
        }

        return appliedMinutes;
    }
}