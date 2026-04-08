package com.hometalk.onepass.entity.dashboard;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;
    private Long userId;
    private String title;
    private LocalDateTime  createdAt;

    @Builder
    public Notice(Long id, Long user_id, String title, LocalDateTime created_at) {
        this.noticeId = id;
        this.userId = user_id;
        this.title = title;
        this.createdAt = created_at;
    }
}
