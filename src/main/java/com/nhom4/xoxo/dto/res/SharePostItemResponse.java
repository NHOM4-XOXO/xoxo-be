package com.nhom4.xoxo.dto.res;

public record SharePostItemResponse(
	Long id, String shareContent,
	Long originalPostId,
	Long sharerId, String sharerFirstName, String sharerLastName, String sharerAvatarUrl,
	Integer likeCount, Integer commentCount, Integer shareCount, Integer viewCount,
	java.time.LocalDateTime createdAt
) {}
