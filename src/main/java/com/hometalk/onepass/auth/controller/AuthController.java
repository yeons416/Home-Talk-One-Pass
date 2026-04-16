package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.dto.SignUpDTO;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.auth.service.SignUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final SignUpService signUpService;

    @GetMapping("")
    public String Auth() {
        return "auth/login";
    }


    @GetMapping("register")
    public String Resister(Model model) {
        // step = 1로 초기값 설정
        model.addAttribute("step", 1);

        // 2. 타임리프 th:object와 연결될 빈 DTO 객체 전달
        model.addAttribute("signUpDTO", new SignUpDTO());

        return "auth/register";
    }

    @PostMapping("register/signup")   // 회원가입 단계별 목록 처리
    public String signup(
            @ModelAttribute("signUpDTO") SignUpDTO signUpDTO,      // DTO
            @RequestParam(required = false, defaultValue = "next") String action, // 버튼 상태
            @RequestParam(defaultValue = "1") int currentStep,  // 회원가입 단계
            Model model
    ) {
        if ("next".equals(action)) {
            model.addAttribute("step", currentStep + 1);
            return "auth/register"; // 본인의 html 파일명
        }

        if ("prev".equals(action)) {
            model.addAttribute("step", currentStep - 1);
            return "auth/register";
        }

        if ("complete".equals(action)) {
            // 최종 서비스 로직 호출 (회원가입 처리)
            signUpService.signUp(signUpDTO);
            return "redirect:/auth";
        }

        return "auth/register";
    }
}