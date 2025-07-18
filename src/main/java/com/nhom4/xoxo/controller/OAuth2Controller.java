package com.nhom4.xoxo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class OAuth2Controller {

    @GetMapping("/login")
    public String login() {
        // Redirect to Google OAuth2
        return "redirect:/oauth2/authorization/google";
    }

    @GetMapping("/oauth2/success")
    public String oauth2Success() {
        return "OAuth2 login successful!";
    }
} 