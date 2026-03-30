package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class PostResponse {
    private Long postId;
    private String title;
    private String content;
    private boolean pinned;
    //private String boardName;
    //private String categoryName;
    //private String writerNickname;
    //private List<String> tags;
    private LocalDateTime createdAt;
    private int viewCount;
    private int commentCount;

    // Entity -> DTO 변환 생성자
    public PostResponse(Post post) {
        this.postId = post.getPostId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.pinned = post.isPinned();
        //this.boardName = post.getCategory().getBoard().getBoardName();
        //this.categoryName = post.getCategory().getName();
        //this.writerNickname = post.getWriter().getNickname();
        //this.tags = post.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toList());
        this.createdAt = post.getCreatedAt();
        this.viewCount = post.getViewCount();
        this.commentCount = post.getCommentCount();
    }
}
