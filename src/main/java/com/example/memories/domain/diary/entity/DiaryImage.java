package com.example.memories.domain.diary.entity;

import com.example.memories.global.common.entity.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diary_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryImage extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(name = "image_key", nullable = false)
    private String imageKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    public DiaryImage(Diary diary, String imageKey, int sortOrder) {
        this.diary = diary;
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
    }
}