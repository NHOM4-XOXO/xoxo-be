package com.nhom4.xoxo.dto.res;

import java.time.LocalDateTime;

import com.nhom4.xoxo.enums.PostStatus;
import com.nhom4.xoxo.enums.PostType;

public record PostItemResponse(
	Long id,
	String content,
	PostStatus status,
	PostType type,
	String location,
	String hashtags,
	boolean isPublic,
	boolean allowComments,
	boolean allowLikes,
	boolean allowShares,
	Integer likeCount,
	Integer commentCount,
	Integer shareCount,
	Integer viewCount,
	Long authorId,
	String authorFirstName,
	String authorLastName,
	String authorAvatarUrl,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {}