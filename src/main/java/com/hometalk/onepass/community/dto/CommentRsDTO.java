package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Comment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentRsDTO {
    private Long id;
    private String content;

    private String nickname;

    public static CommentRsDTO from(Comment comment) {
        return CommentRsDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .nickname(comment.getWriter().getNickname())
                .build();
    }
}
