package com.hometalk.onepass.notice.entity;

import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Notice extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Boolean isPinned;

    private int viewCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge", columnDefinition = "VARCHAR(20)")
    private Badge badge;
}