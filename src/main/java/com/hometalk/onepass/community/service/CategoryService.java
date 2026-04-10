package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.dto.CategoryResponseDTO;
import com.hometalk.onepass.community.repository.BoardRepository;
import com.hometalk.onepass.community.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final BoardRepository boardRepository;

    @Transactional
    public List<CategoryResponseDTO> findAllByBoardId(Long boardId) {
        return categoryRepository.findAllByBoardId(boardId).stream()
                .map(CategoryResponseDTO::from)
                .collect(Collectors.toList());
    }

    public CategoryResponseDTO findByCode(String categoryCode) {
        return categoryRepository.findByCode(categoryCode).map(CategoryResponseDTO::from).orElse(null);
    }

    // 글쓰기 모드용
    @Transactional
    public List<CategoryResponseDTO> findAllByBoardIdForWrite(Long boardId) {
        return categoryRepository.findAllByBoardId(boardId).stream()
                .filter(category -> !category.getName().equals("전체")) // '전체' 제외
                .map(CategoryResponseDTO::from)
                .collect(Collectors.toList());
    }
}
