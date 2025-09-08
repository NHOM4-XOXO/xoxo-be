package com.nhom4.xoxo.entity;

import com.nhom4.xoxo.enums.GroupStatus;
import com.nhom4.xoxo.enums.PrivacyLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "`groups`")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Group extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_url", columnDefinition = "TEXT")
    private String coverUrl;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private PrivacyLevel privacy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status;

    @Column(name = "member_count", nullable = false, columnDefinition = "int default 0")
    @Builder.Default
    private Integer memberCount = 0;

    @Column(name = "post_count", nullable = false, columnDefinition = "int default 0")
    @Builder.Default
    private Integer postCount = 0;

    @Column(name = "is_verified", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON array of tags

    @Column(name = "location", columnDefinition = "TEXT")
    private String location;

    @Column(name = "website", columnDefinition = "TEXT")
    private String website;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = GroupStatus.ACTIVE;
        }
        if (memberCount == null) {
            memberCount = 0;
        }
        if (postCount == null) {
            postCount = 0;
        }
        if (isVerified == null) {
            isVerified = false;
        }
    }
}
