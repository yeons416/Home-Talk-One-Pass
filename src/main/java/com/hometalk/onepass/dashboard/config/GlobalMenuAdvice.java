package com.hometalk.onepass.dashboard.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalMenuAdvice {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        // 모든 페이지 요청 시 현재 URI를 추출하여 'currentUri'라는 이름으로 모델에 담습니다.
        return request.getRequestURI();
    }
}
