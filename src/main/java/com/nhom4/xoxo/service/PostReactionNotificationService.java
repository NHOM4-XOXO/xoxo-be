package com.nhom4.xoxo.service;

import com.nhom4.xoxo.entity.Notification;
import com.nhom4.xoxo.entity.NotificationType;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PostReactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostReactionNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public void sendReactionAddedNotification(Post post, User reactor, PostReactionType reactionType) {
        try {
            Map<String, Object> realtimeData = Map.of(
                "type", "POST_REACTION_ADDED",
                "postId", post.getId(),
                "userId", reactor.getId(),
                "userName", reactor.getFirstName() + " " + reactor.getLastName(),
                "userAvatar", reactor.getAvatarUrl() != null ? reactor.getAvatarUrl() : "",
                "reactionType", reactionType.name(),
                "emoji", reactionType.getEmoji(),
                "displayName", reactionType.getDisplayName(),
                "timestamp", System.currentTimeMillis()
            );

            // Send real-time update to post watchers
            messagingTemplate.convertAndSend("/topic/post/" + post.getId() + "/reactions", realtimeData);

            // Send notification to post author (if not self-reaction)
            if (!post.getAuthor().getId().equals(reactor.getId())) {
                messagingTemplate.convertAndSendToUser(
                    post.getAuthor().getId().toString(),
                    "/queue/post-notifications",
                    realtimeData
                );

                // Create persistent notification
                String notificationMessage = reactor.getFirstName() + " " + reactor.getLastName() + 
                    " đã " + reactionType.getDisplayName().toLowerCase() + " bài viết của bạn";
                
                Notification notification = Notification.builder()
                    .userId(post.getAuthor().getId())
                    .senderId(reactor.getId())
                    .type(NotificationType.POST_LIKE) 
                    .targetType("POST")
                    .targetId(post.getId())
                    .actionType("REACTION")
                    .message(notificationMessage)
                    .isRead(false)
                    .build();
                
                notificationService.createNotification(notification);
            }

        } catch (Exception e) {
            log.error("Error sending reaction notification for post {}: {}", post.getId(), e.getMessage());
        }
    }

    public void sendReactionRemovedNotification(Post post, User reactor, PostReactionType reactionType) {
        try {
            Map<String, Object> realtimeData = Map.of(
                "type", "POST_REACTION_REMOVED",
                "postId", post.getId(),
                "userId", reactor.getId(),
                "reactionType", reactionType.name(),
                "timestamp", System.currentTimeMillis()
            );

            // Send real-time update to post watchers
            messagingTemplate.convertAndSend("/topic/post/" + post.getId() + "/reactions", realtimeData);

        } catch (Exception e) {
            log.error("Error sending reaction removal notification for post {}: {}", post.getId(), e.getMessage());
        }
    }

    public void sendReactionStatsUpdate(Long postId, Map<PostReactionType, Long> reactionStats, long totalCount) {
        try {
            Map<String, Object> statsUpdate = Map.of(
                "type", "REACTION_STATS_UPDATE",
                "postId", postId,
                "reactionStats", reactionStats,
                "totalReactions", totalCount,
                "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSend("/topic/post/" + postId + "/stats", statsUpdate);

        } catch (Exception e) {
            log.error("Error sending reaction stats update for post {}: {}", postId, e.getMessage());
        }
    }
}
