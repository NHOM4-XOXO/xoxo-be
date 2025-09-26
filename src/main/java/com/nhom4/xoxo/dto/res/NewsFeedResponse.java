package com.nhom4.xoxo.dto.res;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for NewsFeed with pagination and metadata
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsFeedResponse {
    
    private List<NewsFeedItemResponse> items;
    private Integer currentPage;
    private Integer totalPages;
    private Long totalElements;
    private Integer pageSize;
    private Boolean hasNext;
    private Boolean hasPrevious;
    private Long unseenCount;
    private Boolean isFirstPage;
    private Boolean isLastPage;
    
    // Additional metadata
    private String cacheStatus; // "HIT" or "MISS" 
    private Long loadTimeMs;
    private String lastUpdated;
}

