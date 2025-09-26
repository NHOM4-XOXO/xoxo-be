package com.nhom4.xoxo.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.nhom4.xoxo.enums.NewsFeedItemType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NewsFeedItem entity represents individual items in a user's news feed.
 * This includes posts from friends, group activities, user activities, etc.
 * Each user has their own personalized feed items.
 */
@Entity
@Table(name = "news_feed_items", indexes = {
    @Index(name = "idx_user_created_at", columnList = "user_id, created_at"),
    @Index(name = "idx_user_type_created_at", columnList = "user_id, item_type, created_at"),
    @Index(name = "idx_post_created_at", columnList = "post_id, created_at"),
    @Index(name = "idx_actor_created_at", columnList = "actor_id, created_at")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewsFeedItem extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The user who owns this feed item (whose feed this item appears in)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * The user who performed the action (posted, liked, commented, etc.)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;
    
    /**
     * Type of news feed item (POST, LIKE, COMMENT, FRIENDSHIP, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private NewsFeedItemType itemType;
    
    /**
     * Related post (for post-related activities)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
    
    /**
     * Related group (for group-related activities)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;
    
    /**
     * Related user (for user-related activities like new friendship)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;
    
    /**
     * Priority score for ranking feed items (higher = more important)
     * Based on factors like: recency, relationship strength, interaction history
     */
    @Column(name = "priority_score", nullable = false)
    @Builder.Default
    private Double priorityScore = 1.0;
    
    /**
     * Whether the user has seen this item
     */
    @Column(name = "is_seen", nullable = false)
    @Builder.Default
    private Boolean isSeen = false;
    
    /**
     * Whether the user has interacted with this item
     */
    @Column(name = "is_interacted", nullable = false)
    @Builder.Default
    private Boolean isInteracted = false;
    
    /**
     * Timestamp when the original activity occurred
     */
    @Column(name = "activity_time", nullable = false)
    private LocalDateTime activityTime;
    
    /**
     * Additional metadata as JSON string
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        NewsFeedItem other = (NewsFeedItem) obj;
        return Objects.equals(id, other.id);
    }
}

