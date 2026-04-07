package com.hometalk.onepass.entity.dashboard;

import lombok.Builder;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

public class Community {

    private Long userId;
    private int categoryId;
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
