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
public class GroupAnalyticsResponse {
    private Long groupId;
    private String groupName;
    private Integer totalMembers;
    private Integer totalPosts;
    private Integer activeMembers; // Members who posted/commented in last 30 days
    private Integer newMembersThisMonth;
    private Integer postsThisMonth;
    private Integer commentsThisMonth;
    private Map<String, Integer> membersByCountry;
    private Map<String, Integer> postsByDay; // Last 7 days
    private Double engagementRate;
    private LocalDateTime lastUpdated;
}






