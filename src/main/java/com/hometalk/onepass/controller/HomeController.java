package com.hometalk.onepass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/*
    홈페이지 컨트롤러
        -> 프로젝트 메인 랜딩 페이지
        URL: GET  /hometop/ 또는 /hometop/home => 랜딩 페이지 요청은 두개 다 받는 것으로!
        템플릿 : templeates/home.html

    비로그인 시 어떻게 해야할 것인가? -> 모든 메뉴가 노출될 필요 없음. -> home.html (랜딩)
    로그인 상태일 때 화면이 나와야할 것인가? -> redirect:/ dashboard
    => 생각해보기
*/
@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // 로그인한 사용자는 대시보드로 redirect

        // 시드 데이터 - 화면에서 참조할 수 있었야함.(관련 데이터 공유 - 추후 추가)
        return "home";
    }
}

