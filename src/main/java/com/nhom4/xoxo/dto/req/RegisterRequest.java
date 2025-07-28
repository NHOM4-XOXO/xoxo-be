package com.nhom4.xoxo.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Email không hợp lệ")
    @Schema(example = "example@example.com", description = "Email của người dùng")
    private String email;
    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ cái viết hoa, chữ cái viết thường, số và ký tự đặc biệt")
    private String password;
    @NotBlank(message = "Tên không được để trống")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Tên không được chứa số hoặc ký tự đặc biệt")
    private String firstName;
    @NotBlank(message = "Họ không được để trống")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Họ không được chứa số hoặc ký tự đặc biệt")
    private String lastName;

 
} 