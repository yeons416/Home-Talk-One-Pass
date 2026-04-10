package com.hometalk.onepass.dashboard.entity.dashboard;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

public class Community {

    private Long userId;
    private int categoryId;

    @Id // 1. PK로 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 2. DB에서 자동 증가(Auto-increment)한다면 추가
    private Long postId;

    private String title;
    private LocalDateTime createdAt;

    @Builder
    public Community(Long userId, Integer categoryId, Long postId, String title, LocalDateTime createdAt) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.postId = postId;
        this.title = title;
        this.createdAt = createdAt;
    }
}
