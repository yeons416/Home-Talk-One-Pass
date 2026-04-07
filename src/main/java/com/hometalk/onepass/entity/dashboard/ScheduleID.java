package com.hometalk.onepass.entity.dashboard;

import lombok.Builder;

import java.time.LocalDateTime;

public class ScheduleID {

    private Long userId;
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
