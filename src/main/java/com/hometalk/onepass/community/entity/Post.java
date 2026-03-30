package com.hometalk.onepass.community.entity;

import com.hometalk.onepass.community.dto.PostUpdateRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;
    private String title;
    @Column(columnDefinition = "Text")
    private String content;
    private boolean pinned = false;
    private int viewCount = 0;
    private int commentCount = 0;

    // FK들 (후에 Entity 구현하면 연결할 것)
    private Long writerId;
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    private PostStatus status = PostStatus.ACTIVE;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // 자동 시간 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    public void update(PostUpdateRequest dto) {
        this.title = dto.getTitle();
        this.content = dto.getContent();
        this.pinned = dto.isPinned();
    }
}
