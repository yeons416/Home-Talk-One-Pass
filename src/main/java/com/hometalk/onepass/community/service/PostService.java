package com.hometalk.onepass.community.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.community.dto.PostRequestDTO;
import com.hometalk.onepass.community.dto.PostListResponse;
import com.hometalk.onepass.community.dto.PostResponseDTO;
import com.hometalk.onepass.community.dto.PostUserRsDTO;
import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Category;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.repository.BoardRepository;
import com.hometalk.onepass.community.repository.CategoryRepository;
import com.hometalk.onepass.community.repository.PostRepository;
import com.hometalk.onepass.community.validator.PostValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostValidator postValidator;
    private final CategoryRepository categoryRepository;
    private final BoardRepository boardRepository;

    // Create
    @Transactional
    public Long postSave(String boardCode, PostRequestDTO dto, Long userId) { // userId 파라미터 추가
        Board board = boardRepository.findByCode(boardCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시판이 존재하지 않습니다. " + boardCode));
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리가 존재하지 않습니다. " + dto.getCategoryId()));
        // 작성자 정보 조회
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Post post = dto.toEntity(category, board, writer);
        return postRepository.save(post).getId();
    }

    // Read
    public List<PostListResponse> postList(Long boardId, Long categoryId, int page) {
        List<Post> posts;
        if (categoryId == null) {
            // 게시판 전체 조회 (최신순)
            posts = postRepository.findAllByBoard_IdOrderByIdDesc(boardId);
        } else {
            // 특정 카테고리 조회 (최신순)
            posts = postRepository.findAllByBoard_IdAndCategory_IdOrderByIdDesc(boardId, categoryId);
        }

        return posts.stream().map(PostListResponse::new).toList();
    }
/*  필터링

    @Transactional
    public List<PostListResponse> getPostsByCondition(Long boardId, Long categoryId) {
        List<Post> posts;

        if (categoryId != null) {
            // 특정 카테고리 글만 조회
            posts = postRepository.findAllByCategoryIdOrderByIdDesc(categoryId);
        } else if (boardId != null) {
            // 특정 게시판의 모든 카테고리 글 조회
            posts = postRepository.findAllByCategoryBoardIdOrderByIdDesc(boardId);
        } else {
            // 전체 조회 (관리자용 등)
            posts = postRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        }

        return posts.stream()
                .map(PostListResponse::new)
                .collect(Collectors.toList());
    }*/

    // Read - 상세 페이지
    public PostResponseDTO postDetail(Long postId, PostUserRsDTO currentUser) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        PostResponseDTO dto = new PostResponseDTO(post);
        postValidator.setAuthority(dto, post, currentUser);
        return dto;
    }

    // Update
    // 수정 화면에 데이터를 가져오기
    public PostRequestDTO getPostForEdit(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + id));

        // 엔티티를 바로 RequestDTO로 변환해서 반환
        return PostRequestDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .pinned(post.isPinned())
                .build();
    }
    @Transactional
    public void postUpdate(Long id, PostRequestDTO dto, Long userId) throws AccessDeniedException {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        // 작성자 검증
        postValidator.validateOwner(post, userId);
        post.update(dto);
    }

    // Delete
    @Transactional
    public void deletePost(Long id, Long currentUserId) {   // user merge 후에는 userId도 필요
        // 게시글 조회
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        // 권한 검증
        postValidator.validateOwner(post, currentUserId);
        post.softDelete();
    }

}
