package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.dto.BoardRequestDTO;
import com.hometalk.onepass.community.dto.BoardResponseDTO;
import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;

    public List<BoardResponseDTO> findAll() {
        return boardRepository.findAll().stream()
            .map(board -> new BoardResponseDTO(board))
            .collect(Collectors.toList());
    }

    public BoardResponseDTO findById(Long id) {
        return boardRepository.findById(id).map(board -> new BoardResponseDTO(board)).orElse(null);
    }

    public BoardResponseDTO findByCode(String code) {
        return boardRepository.findByCode(code).map(board -> new BoardResponseDTO(board)).orElse(null);
    }

    public BoardResponseDTO findByName(String name) {
        return boardRepository.findByName(name).map(board -> new BoardResponseDTO(board)).orElse(null);
    }

    // 게시판 생성
    @Transactional
    public Board save(BoardRequestDTO boardRequestDTO) {
        Board board = new Board();
        board.setName(boardRequestDTO.getName());
        return boardRepository.save(board);
    }
}
