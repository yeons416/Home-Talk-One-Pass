package com.hometalk.onepass.dashboard.entity.dashboard;

import lombok.Builder;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

public class ScheduleID {

    private Long userId;

    @Id // 1. PK 지정
    private Long scheduleId;

    private String title;
    private LocalDateTime startAt;

    @Builder
    public ScheduleID(Long userId, Long scheduleId, String title, LocalDateTime startAt) {
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.title = title;
        this.startAt = startAt;
    }
}
