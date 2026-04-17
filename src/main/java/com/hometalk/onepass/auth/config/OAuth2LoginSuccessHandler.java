/*
package com.hometalk.onepass.auth.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        // 카카오에서 받아온 이메일이나 고유 ID 추출
        String email = (String) ((Map<String, Object>) oAuth2User.getAttributes().get("kakao_account")).get("email");

        // DB 확인 로직 (예시)
        boolean isFirstLogin = true; // 실제로는 userService.findByEmail(email).isEmpty() 등으로 체크

        // 처음 로그인하는 유저라면 추가 정보 페이지로 보냄
        getRedirectStrategy().sendRedirect(request, response, "/signup/extra?email=" + email);
    }
}*/
