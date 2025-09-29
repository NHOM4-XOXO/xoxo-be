package com.nhom4.xoxo.dto.req;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nhom4.xoxo.enums.GenderStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @Size(max = 255)
    @NotBlank(message = "Tên không được để trống")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Tên không được chứa số hoặc ký tự đặc biệt")
    private String firstName;

    @Size(max = 255)
    @NotBlank(message = "Họ không được để trống")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Họ không được chứa số hoặc ký tự đặc biệt")
    private String lastName;


    @NotNull(message = "Giới tính không được để trống")
    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private GenderStatus gender;

    @Size(max = 255)
    @NotBlank(message = "Bio không được để trống")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Bio không được chứa số hoặc ký tự đặc biệt")
    private String bio;

    @Size(max = 255)
    @NotBlank(message = "Username không được để trống")
    @Pattern(regexp = "^[\\p{L}\\p{N} ]+$", message = "Username không được chứa ký tự đặc biệt")
    private String username;

 
    
}
