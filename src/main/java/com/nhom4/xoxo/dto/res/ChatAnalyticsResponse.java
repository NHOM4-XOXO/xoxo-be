package com.nhom4.xoxo.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatAnalyticsResponse {
    private Long chatRoomId;
    private String chatRoomName;
    
    // Message Statistics
    private Integer totalMessages;
    private Integer messagesThisWeek;
    private Integer messagesThisMonth;
    private Map<String, Integer> messagesByDay; // Last 7 days
    private Map<String, Integer> messagesByType; // TEXT, IMAGE, FILE, etc.
    
    // User Activity
    private Integer totalParticipants;
    private Integer activeParticipants; // Active in last 7 days
    private Map<String, Integer> messagesByUser; // userId -> message count
    private Map<String, Integer> reactionsByUser; // userId -> reaction count
    
    // Engagement Metrics
    private Double averageResponseTime; // in minutes
    private Double messageReadRate; // percentage of messages read
    private Integer totalReactions;
    private Map<String, Integer> popularReactions; // reaction -> count
    
    // Peak Activity
    private Map<String, Integer> messagesByHour; // 0-23 hours
    private String peakActivityHour;
    private String mostActiveUser;
    
    // Media Statistics
    private Integer totalImages;
    private Integer totalFiles;
    private Integer totalVideos;
    private Long totalMediaSizeBytes;
    
    private LocalDateTime lastUpdated;
}





