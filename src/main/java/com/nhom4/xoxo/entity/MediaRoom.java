package com.nhom4.xoxo.entity;

import java.time.LocalDate;

import com.nhom4.xoxo.enums.MediaRoomTargetType;
import com.nhom4.xoxo.enums.MediaType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media_room")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MediaRoom extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long media_id;

    @Column(nullable = false)
    private Long target_id;

    @Column(nullable = false)
    private MediaRoomTargetType target_type;

}
