package com.hometalk.onepass.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NotificationController {

    @GetMapping({"/notification"})
    public String notification(Model model) {
        // model.addAttribute("menu", "notification"); // 현재 메뉴 이름 전달
        // 시드 데이터 (관련 데이터 모델에 공유 - 추후)
        return "/notification/main";
    }

//    @GetMapping({"/vehicle/reapply"})
//    public String vehicleReapplyPage(Model model) {
//        model.addAttribute("menu", "parking");     // 현재 메뉴 이름 전달
//        // 시드 데이터 (관련 데이터 모델에 공유 - 추후)
//        return "parking/vehicle-reapply";
//    }
}
