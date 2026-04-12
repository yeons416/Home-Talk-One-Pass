package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.dto.SignUpDTO;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
@RequiredArgsConstructor // final 필드인 userRepository, authService를 위한 생성자를 자동으로 만듭니다.
public class AuthController {

    private final UserRepository userRepository;
    private final AuthService authService;

    @GetMapping("/auth")
    public String Auth() {
        return "auth/login";
    }

    @GetMapping("/auth/register")
    public String Resister() {
        return "auth/register";
    }

    @PostMapping("/auth/register/signup")
    public String signup(SignUpDTO signupDTO) {
        log.info("회원가입 요청 DTO: {}", signupDTO);

        authService.signUp(signupDTO);

        return "redirect:/auth";
    }
}