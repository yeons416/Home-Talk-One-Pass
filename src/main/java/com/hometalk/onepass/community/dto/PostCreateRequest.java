package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Post;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PostCreateRequest {
    private String title;
    private String content;
    private Long writerId;
    private Long categoryId;
    private boolean pinned;

    public Post toEntity() {
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setPinned(pinned);
        post.setWriterId(writerId);
        post.setCategoryId(categoryId);
        return post;
    }
}
