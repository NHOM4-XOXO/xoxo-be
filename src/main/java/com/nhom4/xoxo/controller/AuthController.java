package com.nhom4.xoxo.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom4.xoxo.constant.WrapResStatus;
import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.ForgotPasswordRequest;
import com.nhom4.xoxo.dto.req.LoginRequest;
import com.nhom4.xoxo.dto.req.RegisterRequest;
import com.nhom4.xoxo.dto.req.ResetPasswordRequest;
import com.nhom4.xoxo.dto.res.LoginResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.entity.VerificationToken;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.repository.VerificationTokenRepository;
import com.nhom4.xoxo.security.JwtTokenProvider;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

   
    public AuthController(VerificationTokenRepository verificationTokenRepository, UserRepository userRepository, UserService userService, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
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
        userService.registerUser(request);
        return ResponseEntity.ok(WrapRes.success("Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản."));
    }

    @Operation(
        summary = "Đăng nhập, nhận JWT token",
        description = "Ai cũng có thể gọi. Trả về JWT token nếu đăng nhập thành công.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công")
        }
    )
    @PostMapping("/login")
    public ResponseEntity<WrapRes<?>> login(@RequestBody @Valid LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow(() -> new NotFoundException("User not found"));
        return ResponseEntity.ok(WrapRes.success(new LoginResponse(jwt, user.getEmail(), user.getRoles().toString())));
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
        Optional<VerificationToken> optionalToken = verificationTokenRepository.findByToken(token);
        if (optionalToken.isEmpty()) {
            return ResponseEntity.status(401).body(WrapRes.error(WrapResStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn."));
        }
        VerificationToken verificationToken = optionalToken.get();
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(401).body(WrapRes.error(WrapResStatus.UNAUTHORIZED, "Token đã hết hạn."));
        }
        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);
        return ResponseEntity.ok(WrapRes.success("Xác thực tài khoản thành công. Bạn có thể đăng nhập!"));
    }

    // quen mat khau
    @Operation(
        summary = "Quên mật khẩu",
        description = "Ai cũng có thể gọi. Gửi email để reset mật khẩu.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Gửi email reset mật khẩu thành công")
        }
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<WrapRes<?>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ResponseEntity.ok(WrapRes.success("Gửi email reset mật khẩu thành công. Vui lòng kiểm tra email!"));
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
} 