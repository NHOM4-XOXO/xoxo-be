package com.nhom4.xoxo.controller;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.UpdateUserRequest;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;


@RestController
@RequestMapping("/api/user")
public class UserController {

   
    private final UserService userService;

    private final ModelMapper modelMapper;

    public UserController(UserService userService, ModelMapper modelMapper) {
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

  
    @Operation(
        summary = "Lấy thông tin cá nhân của user hiện tại",
        description = "Yêu cầu đã đăng nhập. Trả về thông tin user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lấy thông tin user thành công")
        }
    )
    @GetMapping("/profile")
    public ResponseEntity<WrapRes<?>> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        return ResponseEntity.ok(WrapRes.success(userResponse));
    }

    
    @Operation(
        summary = "Cập nhật thông tin cá nhân của user hiện tại",
        description = "Yêu cầu đã đăng nhập. Cập nhật thông tin user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thông tin user thành công")
        }
    )
    @PutMapping("/profile")
    public ResponseEntity<WrapRes<?>> updateUserProfile(@RequestBody UpdateUserRequest updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        modelMapper.map(updateRequest, user);
        User updatedUser = userService.updateUser(user, user);
        UserResponse userResponse = modelMapper.map(updatedUser, UserResponse.class);
        return ResponseEntity.ok(WrapRes.success(userResponse));
    }
} 