package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostListResponse {
    private Long postId;
    private String title;
    private boolean pinned;
    //private String boardName;
    //private String categoryName;
    //private String writerNickname;
    private LocalDateTime createdAt;
    private int viewCount;
    private int commentCount;

    public PostListResponse(Post post) {
        this.postId = post.getPostId();
        this.title = post.getTitle();
        this.pinned = post.isPinned();
        //this.boardName = post.getCategory().getBoard().getBoardName();
        //this.categoryName = post.getCategory().getName();
        //this.writerNickname = post.getWriter().getNickname();
        this.createdAt = post.getCreatedAt();
        this.viewCount = post.getViewCount();
        this.commentCount = post.getCommentCount();
    }
}
