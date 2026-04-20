package com.hometalk.onepass.community.controller;

/*
    게시글 상태 변경, 상단 고정, 조회수, 좋아요/추천 기능
 */

import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.service.PostActionService;
import com.hometalk.onepass.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostActionController {
    private final PostActionService postActionService;

    // 1. 공지 고정 토글 (관리자용)
    @PostMapping("/{postId}/pin")
    public String togglePin(@PathVariable Long postId, @RequestParam Long adminId) {
        postActionService.togglePin(postId, adminId);
        return "redirect:/community/post/" + postId; // 다시 게시글로
    }

    // 2. 나눔 상태 변경 (작성자용)
    @PostMapping("/{postId}/market-status")
    public String updateMarketStatus(@PathVariable Long postId, @RequestParam MarketStatus status) {
        postActionService.updateMarketStatus(postId, status);
        return "redirect:/community/post/" + postId;
    }

    // 3. 관리자 게시글 숨김
    @PostMapping("/{postId}/hide")
    public String hidePost(@PathVariable Long postId) {
        postActionService.hidePost(postId);
        return "redirect:/community/main"; // 목록으로 튕기기
    }
}
