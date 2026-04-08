package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Post;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 1. 게시판 전체 글 조회 (카테고리 무관)
    List<Post> findAllByBoardId(Long boardId);
    // 2. 특정 게시판 내 특정 카테고리 글 조회
    List<Post> findAllByBoardIdAndCategoryId(Long boardId, Long categoryId);

    List<Post> findAllByBoard_IdOrderByIdDesc(Long boardId);

    List<Post> findAllByBoard_IdAndCategory_IdOrderByIdDesc(Long boardId, Long categoryId);
}
