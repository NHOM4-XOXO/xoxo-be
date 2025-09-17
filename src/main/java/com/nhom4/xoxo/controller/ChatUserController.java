package com.nhom4.xoxo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nhom4.xoxo.constant.WrapResStatus;
import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.service.UserService;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat User", description = "APIs for chat user information")
public class ChatUserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Get user info for chat participants", 
               description = "Get basic user information for chat participants. This is a public endpoint for chat functionality.")
    @ApiResponse(responseCode = "200", description = "User information retrieved successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/users/{userId}")
    public ResponseEntity<WrapRes<UserResponse>> getChatUserInfo(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(404)
                    .body(WrapRes.error(WrapResStatus.NOT_FOUND, "User not found"));
            }
            
            // Create a basic user response for chat purposes
            UserResponse userResponse = new UserResponse();
            userResponse.setId(user.getId());
            userResponse.setFirstName(user.getFirstName());
            userResponse.setLastName(user.getLastName());
            userResponse.setUsername(user.getUsername());
            userResponse.setEmail(user.getEmail());
            userResponse.setAvatarUrl(user.getAvatarUrl());
        
            
            return ResponseEntity.ok(WrapRes.success(userResponse));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(WrapRes.error(WrapResStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
        }
    }
}
