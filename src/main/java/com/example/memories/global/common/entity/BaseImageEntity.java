package com.example.memories.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public class BaseImageEntity extends CreatedAtEntity{
    @Column(name = "image_key", nullable = false)
    private String imageKey;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
