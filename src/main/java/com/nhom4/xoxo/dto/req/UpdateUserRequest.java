package com.nhom4.xoxo.dto.req;

import java.time.LocalDate;

import com.nhom4.xoxo.enums.GenderStatus;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserRequest {
    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String lastName;

    private LocalDate dateOfBirth;

    private GenderStatus gender;

    @Size(max = 255)
    private String avatarUrl;

    @Size(max = 255)
    private String coverUrl;

    @Size(max = 500)
    private String bio;
    
}
