package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.community.dto.*;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.repository.PostRepository;
import com.hometalk.onepass.community.service.BoardService;
import com.hometalk.onepass.community.service.CategoryService;
import com.hometalk.onepass.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
                            @RequestParam(defaultValue = "1") int page,
                            Model model) {
        BoardResponseDTO board = boardService.findByCode(boardCode);
        // 사용자의 첫 페이지(1)은 JPA에서 0으로 처리하므로 1씩 빼줘야 함
        int pageIndex = (page < 1) ? 0 : page - 1;
        return fillCommunityModel(board, null, page, model);
    }

    // 카테고리별 목록
    @GetMapping("/{boardCode}/{categoryCode:[a-zA-Z]+}")
    public String categoryList(@PathVariable String boardCode,
                               @PathVariable String categoryCode,
                               @RequestParam(defaultValue = "1") int page,
                               Model model) {
        BoardResponseDTO board = boardService.findByCode(boardCode);
        CategoryResponseDTO category = "all".equals(categoryCode) ? null
                                        : categoryService.findByCode(categoryCode);
        int pageIndex = (page < 1) ? 0 : page - 1;

        return fillCommunityModel(board, category, page, model);
    }

    // 게시글 상세 페이지
/*  CustomUserDetails 같은 스프링 시큐리티 구현 후 주석 해제
    @GetMapping("/{boardCode}/{id:[0-9]+}")
    public String postDetail(@PathVariable String boardCode, @PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails userDetails, // 추가
                             Model model) {

        // 로그인 안 한 경우도 고려하여 DTO 구성 (userDetails가 null일 수 있음)
        PostUserRsDTO currentUser = (userDetails != null) ?
                new PostUserRsDTO(userDetails.getUser()) : null;

        // 서비스에 currentUser 전달
        PostResponseDTO post = postService.postDetail(id, currentUser);
        model.addAttribute("post", post);

        BoardResponseDTO board = boardService.findByCode(boardCode);
        addLayoutAttributes(board, null, model, false);
        return "community/postDetail";
    }
 */
    @GetMapping("/{boardCode}/{id:[0-9]+}")
    public String postDetail(@PathVariable String boardCode, @PathVariable Long id, Model model) {
        // [임시] 아직 로그인 연동 전이므로 테스트용 유저 정보 직접 생성
        PostUserRsDTO tempUser = PostUserRsDTO.builder()
                .id(1L)           // 테스트하고 싶은 유저 ID
                .role("MEMBER")   // 또는 "ADMIN"
                .build();

        // 1. 게시글 데이터 가져오기 (tempUser를 넘겨서 editable, admin 여부를 계산함)
        PostResponseDTO post = postService.postDetail(id, tempUser);
        model.addAttribute("post", post);

        // 2. 공통 레이아웃 데이터
        BoardResponseDTO board = boardService.findByCode(boardCode);
        addLayoutAttributes(board, null, model, false);

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

        int tempCount = postService.getTempPostCount(boardCode);
        model.addAttribute("tempCount", tempCount);
        return "community/postForm";
    }

    // 게시글 수정 폼
    @GetMapping("/{boardCode}/edit/{id}")
    public String postForm(@PathVariable String boardCode,
                           @RequestParam(required = false) Long id,
                           Model model) {
        // 공통 레이아웃(배너) 데이터
        BoardResponseDTO board = boardService.findByCode(boardCode);
        addLayoutAttributes(board, null, model, true); // 배너와 헤더는 나오지만 목록은 안 가져옴

        // ID가 있으면 - 임시저장 불러오기
        if (id != null) {
            PostRequestDTO post = postService.getPostForEdit(id);
            model.addAttribute("post", post);
            model.addAttribute("postId", id);
        } else {
            model.addAttribute("post", new PostRequestDTO());
            model.addAttribute("postId", null);
        }
        int tempCount = postService.getTempPostCount(boardCode);
        model.addAttribute("tempCount", tempCount);

        return "community/postForm";
    }

    // 게시글 등록
/*  CustomUserDetails 같은 스프링 시큐리티 구현 후 주석 해제
    @PostMapping("/{boardCode}/save")
    public String createPost(@PathVariable String boardCode, @ModelAttribute PostRequestDTO dto,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        Long id = postService.postSave(boardCode, dto, userId);
        return "redirect:/community/" + boardCode + "/" + id;
    }
 */
    @PostMapping("/{boardCode}/save")
    public String createPost(@PathVariable String boardCode, @ModelAttribute PostRequestDTO dto,
                             @RequestParam(name = "isTemp", defaultValue = "false") boolean isTemp) {
        dto.setPostStatus(isTemp ? PostStatus.DRAFT : PostStatus.ACTIVE);

        // [임시] 아직 로그인 기능이 없으므로, DB에 있는 유저 ID 1번이 작성한다고 가정
        Long tempUserId = 1L;

        // 저장 로직
        Long id = postService.postSave(boardCode, dto, tempUserId);

        // 임시저장일 경우 다시 작성 페이지, 아니면 상세 페이지로 돌아가기
        if (isTemp) {
            return "redirect:/community/" + boardCode + "/write?id=" + id;
        }
        return "redirect:/community/" + boardCode + "/" + id;
    }

    // 게시글 수정
/*  CustomUserDetails 같은 스프링 시큐리티 구현 후 주석 해제
    @PostMapping("/{boardCode}/edit/{id}")      // 폼 태그의 action 주소
    public String updatePost(@PathVariable String boardCode, @PathVariable Long id,
                             PostRequestDTO dto,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {

        postService.postUpdate(id, dto, userDetails.getUserId());
        return "redirect:/community/" + boardCode + "/" + id;
    }
 */
    @PostMapping("/{boardCode}/edit/{id}")
    public String updatePost(@PathVariable String boardCode, @PathVariable Long id, PostRequestDTO dto) {
        // [임시] 수정 권한 테스트를 위한 고정 ID
        Long tempUserId = 1L;

        try {
            postService.postUpdate(id, dto, tempUserId);
        } catch (Exception e) {
            // 권한이 없거나 글이 없는 경우 에러 페이지나 메시지 처리
            return "redirect:/community/" + boardCode + "?error=denied";
        }

        return "redirect:/community/" + boardCode + "/" + id;
    }

    // 게시글 삭제
/*  CustomUserDetails 같은 스프링 시큐리티 구현 후 주석 해제
    @PostMapping("/{boardCode}/delete/{id}")
    public String deletePost(@PathVariable String boardCode, @PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {

        postService.deletePost(id, userDetails.getUserId());
        return "redirect:/community/" + boardCode;
    }
*/
    @PostMapping("/{boardCode}/delete/{id}")
    public String deletePost(@PathVariable String boardCode, @PathVariable Long id) {
        // [임시] 테스트를 위해 1번 유저라고 가정
        Long tempUserId = 1L;

        postService.deletePost(id, tempUserId);
        return "redirect:/community/" + boardCode;
    }

    @GetMapping("/{boardCode}/temp-list")
    @ResponseBody // JSON으로 반환
    public List<PostListResponse> getTempPosts(@PathVariable String boardCode) {
        Long tempUserId = 1L; // 테스트용 ID
        return postService.getTempPosts(boardCode, tempUserId);
    }


    // 공통 데이터 method
    private void addLayoutAttributes(BoardResponseDTO board, CategoryResponseDTO category,
                                     Model model, boolean isWriteMode) {
        if (board == null) return;

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
        if (board == null) {
            return "redirect:/community";    // 게시판 정보 없으면 메인 페이지
        }
        if (category == null) {
            model.addAttribute("categoryCode", "all");
            model.addAttribute("categoryId", null);
        } else {
            model.addAttribute("categoryCode", category.getCode());
            model.addAttribute("categoryId", category.getId());
        }

        // 공통 레이아웃 데이터 채우기
        addLayoutAttributes(board, category, model, false);

        // 목록 페이지 전용 데이터 채우기
        Page<PostListResponse> postsPage = postService.postList(board.getId(),
                                           (category != null ? category.getId() : null),
                                           page);

        model.addAttribute("posts", postsPage.getContent());    // List<PostListReponse>
        model.addAttribute("page", postsPage);                  // 현재 페이지, 총 페이지 등
        model.addAttribute("currentPage", page + 1);                // 현재 페이지 번호
        return "community/postList";
    }
}
