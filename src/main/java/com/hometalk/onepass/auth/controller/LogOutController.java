package com.hometalk.onepass.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // 1. 컨트롤러임을 선언
public class LogOutController {

    // 2. 메서드 이름은 클래스 이름과 다르게 (보통 logout 등으로 설정)
    @GetMapping("/myPage")
    public String logout(HttpServletRequest request, HttpServletResponse response) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            // 3. 실제 로그아웃 처리 루틴 수행
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        String clientId = "4ad79c0bcd4a2a0ab2ac8b3d0fb76b76";
        String redirectUri = "http://localhost:8090/hometop/auth/oauth2/kakao";

        // 4. 로그아웃 후 로그인 페이지로 리다이렉트
        return "redirect:https://kauth.kakao.com/oauth/logout?client_id=" + clientId + "&logout_redirect_uri=" + redirectUri;
    }
}