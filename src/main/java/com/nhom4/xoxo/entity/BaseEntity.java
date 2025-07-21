package com.nhom4.xoxo.entity;

import java.sql.Date;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public class BaseEntity {
    private String updateBy;
    @CreationTimestamp
    private Date updateAt;
    @UpdateTimestamp
    private Date createAt;
}
