package com.nhom4.xoxo.entity;

import com.nhom4.xoxo.enums.GroupMemberStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "group_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember extends BaseEntity {

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMemberId implements Serializable {
        private Long groupId;
        private Long userId;
    }

    @EmbeddedId
    private GroupMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private GroupMemberStatus status; // PENDING, ACCEPTED, REJECTED, BLOCKED
}
