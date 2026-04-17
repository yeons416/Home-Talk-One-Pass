package com.hometalk.onepass.schedule.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCalResponseDto {

    private Long id;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

}