package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDTO {
    private Long id;
    private String title;
    private String content;
    private boolean pinned;
    private String boardName;
    private String categoryName;
    //private String writerNickname;
    //private List<String> tags;
    private LocalDateTime createdAt;
    private boolean isDeleted;
    private int viewCount;
    private int commentCount;

    // Entity -> DTO 변환 생성자
    public PostResponseDTO(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.pinned = post.isPinned();
        this.boardName = post.getCategory().getBoard().getName();
        this.categoryName = post.getCategory().getName();
        //this.writerNickname = post.getWriter().getNickname();
        //this.tags = post.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toList());
        this.createdAt = post.getCreatedAt();
        this.isDeleted = (post.getDeletedAt() != null);
        this.viewCount = post.getViewCount();
        this.commentCount = post.getCommentCount();
    }
}
