package com.nhom4.xoxo.dto.req;


import com.nhom4.xoxo.entity.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleRequest {
    @NotNull(message = "role field is required")
    @Schema(description = "Role của user", example = "ADMIN", allowableValues = {"OWNER", "ADMIN", "USER"})
    private Role role;
    
}
