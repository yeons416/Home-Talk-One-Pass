package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Board;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardRequestDTO {
    private String name;
    private String code;

    public Board toEntity() {
        Board board = new Board();
        board.setName(this.name);
        board.setCode(this.code);
        return board;
    }
}
