package com.nhom4.xoxo.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.nhom4.xoxo.enums.PostStatus;
import com.nhom4.xoxo.enums.PostType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PostStatus status = PostStatus.ACTIVE;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PostType type = PostType.USER_POST;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_post_id")
    private Post parentPost;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "originalPost", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SharePost> shares = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer reactionCount = 0;

    // JSON field để store reaction counts theo type
    @Column(name = "reaction_summary", columnDefinition = "JSON")
    private String reactionSummary; // {"LIKE": 10, "LOVE": 5, "HAHA": 2}

    @Column(nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer shareCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(nullable = true)
    private String location;

    @Column(nullable = true)
    private String hashtags;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean allowComments = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean allowLikes = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean allowShares = true;

    

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
        Post other = (Post) obj;
        return Objects.equals(id, other.id);
    }
}