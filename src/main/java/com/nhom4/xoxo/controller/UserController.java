package com.nhom4.xoxo.controller;

import java.security.Principal;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.UpdateUserRequest;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.service.CloudinaryService;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;
    private final ModelMapper modelMapper;

    public UserController(UserService userService, ModelMapper modelMapper, CloudinaryService cloudinaryService) {
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.cloudinaryService = cloudinaryService;
    }

    @Operation(summary = "Lấy thông tin cá nhân của user hiện tại", description = "Yêu cầu đã đăng nhập. Trả về thông tin user.", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy thông tin user thành công")
    })
    @GetMapping("/profile")
    public ResponseEntity<WrapRes<?>> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        userResponse.setAvatarUrl(cloudinaryService.buildCloudinaryUrl(user.getAvatarUrl()));
        userResponse.setCoverUrl(cloudinaryService.buildCloudinaryUrl(user.getCoverUrl()));
        return ResponseEntity.ok(WrapRes.success(userResponse));
    }

    @Operation(summary = "Cập nhật thông tin cá nhân của user hiện tại", description = "Yêu cầu đã đăng nhập. Cập nhật thông tin user.", responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thông tin user thành công")
    })
    @PutMapping("/profile")
    public ResponseEntity<WrapRes<?>> updateUserProfile(@RequestBody @Valid UpdateUserRequest updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        modelMapper.map(updateRequest, user);
        User updatedUser = userService.updateUser(user, user);
        UserResponse userResponse = modelMapper.map(updatedUser, UserResponse.class);
        return ResponseEntity.ok(WrapRes.success(userResponse));
    }

    @Operation(summary = "Cập nhật ảnh đại diện của user hiện tại", description = "Yêu cầu đã đăng nhập. Cập nhật ảnh đại diện của user.", responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật ảnh đại diện của user thành công")
    })
    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateAvatar(@RequestParam("file") MultipartFile file, Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);
        long maxSize = 2 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest().body("File quá lớn! Vui lòng chọn ảnh nhỏ hơn 2MB.");
        }
        String avatarUrl = cloudinaryService.uploadImage(file, "avatars");
        userService.updateAvatar(user, avatarUrl);
        return ResponseEntity.ok(WrapRes.success("Avatar updated successfully"));
    }

    @Operation(summary = "Cập nhật ảnh bìa của user hiện tại", description = "Yêu cầu đã đăng nhập. Cập nhật ảnh bìa của user.", responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật ảnh bìa của user thành công")
    })
    @PostMapping(value = "/profile/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCover(@RequestParam("file") MultipartFile file, Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);
        long maxSize = 2 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest().body("File quá lớn! Vui lòng chọn ảnh nhỏ hơn 2MB.");
        }
        String coverUrl = cloudinaryService.uploadImage(file, "covers");
        userService.updateCover(user, coverUrl);
        return ResponseEntity.ok(WrapRes.success("Cover updated successfully"));
    }

    //lay user theo username
    @Operation(summary = "Lấy thông tin user theo username", description = "Lấy thông tin user theo username", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy thông tin user theo username thành công")
    })
    @GetMapping("/{username}")
    public ResponseEntity<WrapRes<?>> getUserByUsername(@PathVariable String username) {
        Optional<User> user = userService.findByUsername(username);
        if(user.isPresent()){
            UserResponse userResponse = modelMapper.map(user.get(), UserResponse.class);
            userResponse.setAvatarUrl(cloudinaryService.buildCloudinaryUrl(user.get().getAvatarUrl()));
            userResponse.setCoverUrl(cloudinaryService.buildCloudinaryUrl(user.get().getCoverUrl()));
            return ResponseEntity.ok(WrapRes.success(userResponse));
        }
        return ResponseEntity.ok(WrapRes.success(user));
    }

}