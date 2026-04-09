package com.hometalk.onepass.community.controller;

/*
    게시글 상태 변경, 상단 고정, 조회수, 좋아요/추천 기능
 */

import com.hometalk.onepass.community.entity.MarketStatus;
import com.hometalk.onepass.community.service.PostActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostActionController {
    // 예시
    private final PostActionService postActionService;

    // 마켓 상태 변경 액션 (판매중 <-> 판매완료)
    @PatchMapping("/{id}/market-status")
    public ResponseEntity<Void> updateMarketStatus(
            @PathVariable Long id,
            @RequestParam MarketStatus status) {  // @AuthenticationPrincipal CustomUserDetails userDetails 구현되면 추가

        postActionService.updateMarketStatus(id, status);
        return ResponseEntity.ok().build();
    }

    // 상단 고정 액션 (관리자용)
    @PatchMapping("/{id}/pin")
    public ResponseEntity<String> togglePin(@PathVariable Long id) {
        // [임시] 관리자 유저 ID
        Long tempAdminId = 1L;

        try {
            postActionService.togglePin(id, tempAdminId);
            return ResponseEntity.ok("상단 고정 상태가 변경되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }

    }
}
