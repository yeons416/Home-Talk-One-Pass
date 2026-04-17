package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Comment;
import lombok.Getter;

@Getter
public class CommentRqDTO {
    private String content;

    public Comment toEntity() {
        return Comment.builder()
                .content(this.content)
                .build();
    }
}
