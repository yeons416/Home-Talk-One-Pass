package com.hometalk.onepass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/auth")
    public String Auth(){

        return "/auth/auth";
    }

    @GetMapping("/auth/register")
    public String Resister(){

        return "auth/register";
    }
}
