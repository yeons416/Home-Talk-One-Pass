package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Board;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResponseDTO {
    private Long id;
    private String name;
    private String code;

    public BoardResponseDTO(Board board) {
        this.id = board.getId();
        this.name = board.getName();
        this.code = board.getCode();
    }
}
