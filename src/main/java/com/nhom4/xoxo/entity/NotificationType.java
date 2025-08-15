package com.nhom4.xoxo.entity;

public enum NotificationType {
    FRIEND_REQUEST,      // Lời mời kết bạn
    FRIEND_ACCEPTED,     // Chấp nhận kết bạn
    POST_LIKE,          // Like bài viết
    POST_COMMENT,       // Comment bài viết
    COMMENT_LIKE,       // Like comment
    POST_SHARE,         // Chia sẻ bài viết
    GROUP_INVITE,       // Lời mời vào nhóm
    GROUP_JOIN,         // Tham gia nhóm
    MENTION,            // Được tag trong bài viết/comment
    SYSTEM_ALERT,       // Thông báo hệ thống
    REMINDER            // Nhắc nhở
}