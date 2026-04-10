package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostActionService {

    private final PostRepository postRepository;

    public void updateMarketStatus(Long postId, MarketStatus status) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        post.updateMarketStatus(status);
    }

    @Transactional
    public boolean togglePin(Long id, Long adminId) {
        // 1. 게시글 조회
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        // 2. 관리자 권한 확인 (임시로 adminId가 1L인 경우만 허용하거나, 나중에 Role로 체크)
        // 지금은 테스트를 위해 간단히 처리
        if (adminId == null || adminId != 1L) {
            throw new IllegalStateException("관리자 권한이 필요합니다.");
        }
        // 3. 상태 반전 (true -> false / false -> true)
        post.togglePinned();
        return post.isPinned();
    }
}
