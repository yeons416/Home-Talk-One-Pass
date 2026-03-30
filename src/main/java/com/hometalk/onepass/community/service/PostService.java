package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.dto.PostCreateRequest;
import com.hometalk.onepass.community.dto.PostUpdateRequest;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostRepository postRepository;

    // Create
    public Long postSave(PostCreateRequest dto) {
        Post post = dto.toEntity();
        return postRepository.save(post).getPostId();
    }

    // Read
    public List<Post> postList() {
        return postRepository.findAll();
    }

    // Read - 상세 페이지
    public Post postDetail(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new RuntimeException("게시글이 없습니다."));
    }

    // Update
    public void postUpdate(Long id, PostUpdateRequest dto) {
        Post post = postRepository.findById(id).orElseThrow(() -> new RuntimeException("게시글이 없습니다."));
        post.update(dto);
    }
}
