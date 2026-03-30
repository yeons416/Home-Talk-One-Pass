package com.hometalk.onepass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/*
    홈 페이지 컨트롤러
        --> 프로젝트 메인 랜딩 페이지
    URL : GET /hometop/ 또는 /hometop/home
    템플릿 : templates/home.html

    비로그인 -> home.html (랜딩)
    로그인 상태 -> redirect: /dashboard
 */
@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // 로그인 한 사용자는 대시보드로 리다이렉트

        // 시드 데이터 (관련 데이터 모델에 공유 - 추후)
        return "home";
    }
}
