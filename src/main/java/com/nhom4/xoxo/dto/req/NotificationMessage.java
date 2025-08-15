package com.nhom4.xoxo.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private Long userId;        // Người nhận notification
    private String message;     // Nội dung thông báo
    private String type;        // Loại notification
    private Long targetId;      // ID object liên quan
    private String targetType;  // Loại object
    private Long senderId;      // Người gửi
    private String actionType;  // Hành động
    private String payload;     // Data bổ sung (JSON)
}