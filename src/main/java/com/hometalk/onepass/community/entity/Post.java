package com.hometalk.onepass.community.entity;

import com.hometalk.onepass.community.dto.PostRequestDTO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor     // Builder 쓸 땐 필수
@Builder
@Table(name = "posts")
@SQLDelete(sql = "UPDATE posts SET deleted_at = CURRENT_TIMESTAMP, status = 'DELETED' WHERE id = ?")        // delete() 호출 시 실행될 SQL 문
@SQLRestriction("deleted_at IS NULL")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Lob @Column(columnDefinition = "Text")
    private String content;
    private boolean pinned = false;
    private int viewCount = 0;
    private int commentCount = 0;

    // FK들 (후에 Entity 구현하면 연결할 것)
    // @ManyToOne @JoinColumn(name = "writer_id", referencedColumnName = "id", nullable = false)
    private Long writerId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", referencedColumnName = "id", nullable = false)
    private Board board;

    @Builder.Default
    @Column(length = 20)
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


    public void update(PostRequestDTO dto) {
        this.title = dto.getTitle();
        this.content = dto.getContent();
        this.pinned = dto.isPinned();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = PostStatus.DELETED;
    }
}
