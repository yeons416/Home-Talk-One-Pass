package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.community.dto.*;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.service.BoardService;
import com.hometalk.onepass.community.service.CategoryService;
import com.hometalk.onepass.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/community")
public class PostController {
    private final PostService postService;
    private final BoardService boardService;
    private final CategoryService categoryService;

    // 게시판 목록
    // 게시판별 메인 (카테고리 '전체' 상태)
    @GetMapping("/{boardCode}")
    public String boardMain(@PathVariable String boardCode,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        // 엔티티가 아닌 DTO를 받음
        BoardResponseDTO board = boardService.findByCode(boardCode);
        return fillCommunityModel(board, null, page, model);
    }

    // 카테고리별 목록
    @GetMapping("/{boardCode}/{categoryCode:[a-zA-Z]+}")
    public String categoryList(@PathVariable String boardCode,
                               @PathVariable String categoryCode,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        BoardResponseDTO board = boardService.findByCode(boardCode);
        CategoryResponseDTO category = categoryService.findByCode(categoryCode);

        return fillCommunityModel(board, category, page, model);
    }

    // 게시글 상세 페이지
    @GetMapping("/{boardCode}/{id:[0-9]+}")
    public String postDetail(@PathVariable String boardCode, @PathVariable Long id, Model model) {
        // 1. 게시글 데이터 가져오기
        PostResponseDTO post = postService.postDetail(id);
        model.addAttribute("post", post);

        // 2. 공통 레이아웃(배너) 데이터
        BoardResponseDTO board = boardService.findByCode(boardCode);
        addLayoutAttributes(board, null, model, false); // 공통 데이터 담기
        return "community/postDetail";
    }

    // 게시글 작성 폼
    @GetMapping("/{boardCode}/write")
    public String postForm(@PathVariable String boardCode, Model model) {
        // 1. URL에서 받은 boardCode로 게시판 정보 조회
        BoardResponseDTO board = boardService.findByCode(boardCode);

        // 2. 공통 레이아웃(배너) 데이터
        addLayoutAttributes(board, null, model, true); // 배너와 헤더는 나오지만 목록은 안 가져옴

        // 3. 폼 입력을 위한 빈 DTO
        model.addAttribute("post", new PostRequestDTO());
        return "community/postForm";
    }

    // 게시글 수정 폼
    @GetMapping("{boardCode}/edit/{id}")
    public String postForm(@PathVariable String boardCode, @PathVariable Long id, Model model) {
        PostRequestDTO post = postService.getPostForEdit(id);

        // 2. 공통 레이아웃(배너) 데이터
        BoardResponseDTO board = boardService.findByCode(boardCode);
        addLayoutAttributes(board, null, model, true); // 배너와 헤더는 나오지만 목록은 안 가져옴

        model.addAttribute("post", post);
        model.addAttribute("postId", id);
        return "community/postForm";
    }

    // 게시글 등록
    @PostMapping("/{boardCode}/save")
    public String createPost(@PathVariable String boardCode, @ModelAttribute PostRequestDTO dto) {
        Long id = postService.postSave(boardCode, dto);
        return "redirect:/community/" + boardCode + "/" + id;
    }

    // 게시글 수정
    @PostMapping("/{boardCode}/edit/{id}")      // 폼 태그의 action 주소
    public String updatePost(@PathVariable String boardCode, @PathVariable Long id, PostRequestDTO dto) {
        postService.postUpdate(id, dto);
        return "redirect:/community/" + boardCode + "/" + id;
    }

    // 게시글 삭제
    @PostMapping("/{boardCode}/delete/{id}")
    public String deletePost(@PathVariable String boardCode, @PathVariable Long id) {
        postService.deletePost(id);
        // 삭제 후 해당 게시판의 목록 리다이렉트
        return "redirect:/community/" + boardCode;
    }


    // 공통 데이터 method
    private void addLayoutAttributes(BoardResponseDTO board, CategoryResponseDTO category,
                                     Model model, boolean isWriteMode) {
        model.addAttribute("board", board);
        model.addAttribute("category", category);
        model.addAttribute("boards", boardService.findAll()); // 게시판 헤더용

        // 글쓰기 모드일 때만 '전체'가 빠진 목록을 가져옴
        List<CategoryResponseDTO> categories;
        if (isWriteMode) {
            categories = categoryService.findAllByBoardIdForWrite(board.getId());
        } else {
            categories = categoryService.findAllByBoardId(board.getId());
        }

        model.addAttribute("categories", categories); // 카테고리 배너용
        model.addAttribute("boardId", board.getId());
        model.addAttribute("categoryId", (category != null) ? category.getId() : null);
    }

    // 공통 method - 배너 가져올 페이지/기능들에 모두 쓰임
    private String fillCommunityModel(BoardResponseDTO board, CategoryResponseDTO category, int page, Model model) {
        // 공통 레이아웃 데이터 채우기
        addLayoutAttributes(board, category, model, false);

        // 목록 페이지 전용 데이터 채우기
        model.addAttribute("posts", postService.postList(board.getId(),
                (category != null ? category.getId() : null), page));
        model.addAttribute("currentPage", page);

        return "community/postList";
    }
}
