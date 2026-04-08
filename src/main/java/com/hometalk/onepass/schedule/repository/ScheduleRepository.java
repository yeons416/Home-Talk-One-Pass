package com.hometalk.onepass.schedule.repository;

import com.hometalk.onepass.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
}