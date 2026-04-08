package com.hometalk.onepass.schedule.entity;

import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Schedule extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long noticeId;

    @Column(nullable = false, length = 100)
    private String title;

    private String info;
    private String location;
    private String referenceUrl;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}