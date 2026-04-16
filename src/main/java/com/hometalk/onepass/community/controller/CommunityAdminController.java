package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.community.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/community/admin")
@RequiredArgsConstructor
public class CommunityAdminController {
    private final BoardService boardService;

    @GetMapping("/management")
    public String adminPage(Model model) {
        // 관리 페이지 열 때 전체 게시판 목록 담기
        model.addAttribute("boards", boardService.findAll());
        return "community/admin/management";
    }
}
