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
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.exception.InvalidBoardCodeException;
import com.hometalk.onepass.community.exception.PostNotFoundException;
import com.hometalk.onepass.community.repository.BoardRepository;
import com.hometalk.onepass.community.repository.CategoryRepository;
import com.hometalk.onepass.community.repository.PostRepository;
import com.hometalk.onepass.community.validator.PostValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    public Long postSave(String boardCode, PostRequestDTO dto, Long userId) {
        Board board = boardRepository.findByCode(boardCode)
                .orElseThrow(() -> new InvalidBoardCodeException(boardCode));
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        }
        // 작성자 정보 조회
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Post post = dto.toEntity(category, board, writer);

        return postRepository.save(post).getId();
    }

    // Read
    public Page<PostListResponse> postList(Long boardId, Long categoryId, int page) {
        System.out.println("조회 요청 - boardId: " + boardId + ", page: " + page + ", status: " + PostStatus.ACTIVE);
        // 1. 상태값 설정
        PostStatus status = PostStatus.ACTIVE;

        // 2. 페이징 정보 설정 (페이지 번호, 한 페이지당 개수, 정렬)
        // 현재 Controller에서 defaultValue="0"으로 넘기고 있으므로 그대로 page 사용
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "id"));

        Page<Post> posts;
        if (categoryId == null) {
            // boardId와 status로 조회 (OrderByIdDesc는 쿼리에 넣거나 리포지토리 메서드명에 추가)
            posts = postRepository.findActivePosts(boardId, status, pageable);
        } else {
            posts = postRepository.findCategoryPosts(boardId, categoryId, status, pageable);
        }

        // Page라 Stream -> DTO -> List 재변환 필요 없이 map 기능을 제공하여 곧바로 사용 가능
        return posts.map(PostListResponse::new);
    }

    // Read - 상세 페이지
    public PostResponseDTO postDetail(Long postId, PostUserRsDTO currentUser, String boardCode) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId, boardCode));
        PostResponseDTO dto = new PostResponseDTO(post);
        postValidator.setAuthority(dto, post, currentUser);
        return dto;
    }

    // 임시저장글 개수
    @Transactional(readOnly = true)
    public int getTempPostCount(String boardCode) {
        return postRepository.countByBoardCodeAndPostStatus(boardCode, PostStatus.DRAFT);
    }

    // Update
    // 수정 화면에 데이터를 가져오기
    public PostRequestDTO getPostForEdit(Long id, String boardCode) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id, boardCode));

        // 엔티티를 바로 RequestDTO로 변환해서 반환
        return PostRequestDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .pinned(post.isPinned())
                .build();
    }
    @Transactional
    public void postUpdate(Long id, PostRequestDTO dto, Long userId, String boardCode) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id, boardCode));
        // 작성자 검증
        postValidator.validateOwner(post, userId);
        post.update(dto);
    }

    // Delete
    @Transactional
    public void deletePost(Long id, Long currentUserId, String boardCode) {   // user merge 후에는 userId도 필요
        // 게시글 조회
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id, boardCode));
        // 권한 검증
        postValidator.validateOwner(post, currentUserId);
        post.softDelete();
    }

    public List<PostListResponse> getTempPosts(String boardCode, Long userId) {
        PostStatus status = PostStatus.DRAFT;
        List<Post> posts = postRepository.findTempPosts(boardCode, userId, status);

        // boardCode와 userId가 일치하고 상태가 DRAFT인 글만 최신순으로 조회
        return postRepository.findTempPosts(boardCode, userId, PostStatus.DRAFT)
                .stream()
                .map(PostListResponse::new)
                .collect(Collectors.toList());
    }

}
