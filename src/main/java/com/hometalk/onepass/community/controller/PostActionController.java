package com.hometalk.onepass.community.controller;

/*
    게시글 상태 변경, 상단 고정, 조회수, 좋아요/추천 기능
 */

import com.hometalk.onepass.community.dto.PostResponseDTO;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.service.PostActionService;
import com.hometalk.onepass.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostActionController {
    // 예시
    private final PostActionService postActionService;
    private final PostService postService;

    // 1. 글쓰기 폼 (새 글 & 임시글 불러오기 통합)
/*    @GetMapping("/{code}/write")
    public String writeForm(@PathVariable String code,
                            @RequestParam(value = "tempId", required = false) Long tempId,
                            Model model) {
        PostResponseDTO postDto;
        if (tempId != null) {
            postDto = postService.findById(tempId);
        } else {
            postDto = new PostResponseDTO();
        }

        model.addAttribute("post", postDto);
        // [중요] 폼 렌더링에 필요한 추가 데이터(카테고리 등) 호출 로직이 서비스에 있어야 합니다.
        // model.addAttribute("categories", postService.getCategoriesByBoardCode(code));
        return "community/postForm";
    }

 */

/*    // 2. 게시글 저장 (등록 & 수정 & 임시저장 통합)
    @PostMapping("/{code}/save")
    public String savePost(@PathVariable String code,
                           @ModelAttribute PostResponseDTO postDto,
                           @RequestParam(value = "isTemp", defaultValue = "false") boolean isTemp) {
        postService.postSave(postDto, isTemp);
        return "redirect:/community/" + code;
    }

    // 3. 상태 변경 같은 API성 작업은 ResponseEntity를 사용하여 처리 가능
    @PatchMapping("/api/{id}/market-status")
    @ResponseBody // @Controller 내에서 JSON 응답을 줄 때 사용
    public ResponseEntity<Void> updateMarketStatus(@PathVariable Long id, @RequestParam MarketStatus status) {
        postActionService.updateMarketStatus(id, status);
        return ResponseEntity.ok().build();
    }

 */
}
