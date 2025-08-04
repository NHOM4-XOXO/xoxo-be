package com.nhom4.xoxo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom4.xoxo.constant.WrapResStatus;
import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.ChangePasswordRequest;
import com.nhom4.xoxo.dto.req.ForgotPasswordRequest;
import com.nhom4.xoxo.dto.req.LoginRequest;
import com.nhom4.xoxo.dto.req.RegisterRequest;
import com.nhom4.xoxo.dto.req.ResetPasswordRequest;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.exception.ServiceException;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "APIs cho đăng ký, đăng nhập, xác thực và quản lý mật khẩu")
public class AuthController {

    private final UserService userService;
  

   
    public AuthController(UserService userService) {
    
        this.userService = userService;
      
    }

    @Operation(
        summary = "Đăng ký tài khoản mới",
        description = "Đăng ký tài khoản, gửi email xác thực. Ai cũng có thể gọi.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Đăng ký thành công")
        }
    )
    @PostMapping("/register")
    public ResponseEntity<WrapRes<?>> register(@RequestBody @Valid RegisterRequest request) {
    
        return ResponseEntity.ok(WrapRes.success(userService.registerUser(request)));
    }

    @Operation(
        summary = "Đăng nhập, nhận JWT token",
        description = "Ai cũng có thể gọi. Trả về JWT token nếu đăng nhập thành công.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công")
        }
    )
    @PostMapping("/login")
    public ResponseEntity<WrapRes<?>> login(@RequestBody @Valid LoginRequest loginRequest, HttpServletResponse response) {
       
        return ResponseEntity.ok(WrapRes.success(userService.login(loginRequest, response)));
    }

    @Operation(
        summary = "Xác thực tài khoản qua email",
        description = "Ai cũng có thể gọi. Xác thực tài khoản bằng token gửi qua email.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Xác thực thành công")
        }
    )
    @GetMapping("/verify")
    public ResponseEntity<WrapRes<?>> verifyAccount(@RequestParam("token") String token) {
        String message;
        try {
            message = userService.verifyAccount(token);
        } catch (NotFoundException | ServiceException e) {
            return ResponseEntity.status(401).body(WrapRes.error(WrapResStatus.UNAUTHORIZED, e.getMessage()));
        }
        return ResponseEntity.ok(WrapRes.success(message));
    }

    @Operation(
        summary = "Quên mật khẩu",
        description = "Gửi email reset password. Nếu user đã có token cũ, sẽ tạo token mới và vô hiệu hóa token cũ.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Email reset password đã được gửi"),
            @ApiResponse(responseCode = "404", description = "Email không tồn tại trong hệ thống"),
            @ApiResponse(responseCode = "500", description = "Lỗi gửi email")
        }
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<WrapRes<String>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ResponseEntity.ok(WrapRes.success("Email reset password đã được gửi"));
    }
    


    // reset mat khau
    @Operation(
        summary = "Reset mật khẩu",
        description = "Ai cũng có thể gọi. Reset mật khẩu bằng token gửi qua email.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Reset mật khẩu thành công")
        }
    )
    @PostMapping("/reset-password")
    public ResponseEntity<WrapRes<?>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok(WrapRes.success("Reset mật khẩu thành công. Bạn có thể đăng nhập!"));
    }

    @Operation(
        summary = "Logout",
        description = "Logout tài khoản",
        responses = {
            @ApiResponse(responseCode = "200", description = "Logout thành công")
        }
    )
    @PostMapping("/logout")
    public ResponseEntity<WrapRes<?>> logout(@CookieValue("refreshToken") String refreshToken) {
        userService.logout(refreshToken);
        return ResponseEntity.ok(WrapRes.success("Logout thành công"));
    }

    @Operation(
        summary = "Refresh token",
        description = "Refresh token",
        responses = {
            @ApiResponse(responseCode = "200", description = "Refresh token thành công")
        }
    )
    @PostMapping("/refresh-token")
    public ResponseEntity<WrapRes<?>> refreshToken(@CookieValue("refreshToken") String refreshToken) {
        String newAccessToken = userService.refreshToken(refreshToken);
        return ResponseEntity.ok(WrapRes.success(newAccessToken));
    }   

    @Operation(
        summary = "Change password",
        description = "Change password",
        responses = {
            @ApiResponse(responseCode = "200", description = "Change password thành công")
        }
    )
    @PostMapping("/change-password")
    public ResponseEntity<WrapRes<?>> changePassword(@RequestBody @Valid ChangePasswordRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        boolean isSuccess = userService.changePassword(request.getOldPassword(), request.getNewPassword(), userDetails);
        if(isSuccess){
            return ResponseEntity.ok(WrapRes.success("Change password thành công"));
        }
        return ResponseEntity.badRequest().body(WrapRes.error(WrapResStatus.BAD_REQUEST, "Change password thất bại"));
    }
} 