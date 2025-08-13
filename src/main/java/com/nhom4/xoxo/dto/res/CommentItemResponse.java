package com.nhom4.xoxo.dto.res;

public record CommentItemResponse(
	Long id, String content, Integer likeCount,
	Long authorId, String authorFirstName, String authorLastName, String authorAvatarUrl,
	Long parentCommentId, Long postId,
	java.time.LocalDateTime createdAt
) {}