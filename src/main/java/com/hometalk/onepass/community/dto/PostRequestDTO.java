package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Category;
import com.hometalk.onepass.community.entity.Post;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequestDTO {
    private Long id;        // 수정 시 필요
    private String title;
    private String content;
    private Long writerId;      // Member Entity 구현 전까지 유지
    private Long categoryId;
    private boolean pinned;

    public Post toEntity(Category category, Board board) {
        return Post.builder().title(this.title)
                .content(this.content).pinned(this.pinned)
                .writerId(this.writerId)
                .category(category)
                .board(board)
                .build();
    }
}
