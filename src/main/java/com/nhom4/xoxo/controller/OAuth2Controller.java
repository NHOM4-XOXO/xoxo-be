package com.nhom4.xoxo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/")
@Slf4j
public class OAuth2Controller {

    @GetMapping("/login")
    public String login() {
        log.info("[OAuth2Controller] Redirecting to Google OAuth2");
        // Redirect to Google OAuth2
        return "redirect:/oauth2/authorization/google";
    }

    @GetMapping("/oauth2/success")
    public String oauth2Success() {
        log.info("[OAuth2Controller] OAuth2 success page accessed");
        return "OAuth2 login successful!";
    }
    
    @GetMapping("/oauth2/error")
    public String oauth2Error(@RequestParam(value = "message", required = false) String message) {
        log.error("[OAuth2Controller] OAuth2 error occurred: {}", message);
        return "OAuth2 login failed: " + (message != null ? message : "Unknown error");
    }
} 