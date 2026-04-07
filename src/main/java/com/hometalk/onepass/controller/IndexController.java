package com.hometalk.onepass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("menu", "index"); // 현재 메뉴 이름 전달
        return "index";     // --> templates/index.html 랜더링
    }
}
