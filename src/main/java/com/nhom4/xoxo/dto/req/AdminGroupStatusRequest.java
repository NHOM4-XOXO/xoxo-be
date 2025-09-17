package com.nhom4.xoxo.dto.req;

import com.nhom4.xoxo.enums.GroupStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminGroupStatusRequest {
    @NotNull(message = "Status cannot be null")
    private GroupStatus status;
    private String adminNotes;
}













