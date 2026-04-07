package com.hometalk.onepass.controller;

import com.hometalk.onepass.entity.auth.User;
import com.hometalk.onepass.repository.auth.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/auth")
    public String Auth(){

        return "auth/login";
    }

    @GetMapping("/auth/register")
    public String Resister(){

        return "auth/register";
    }

    @PostMapping("/auth/register/signin")
    public String signup(User user) {
        userRepository.save(user);
        return "redirect:/auth";

    }

}
