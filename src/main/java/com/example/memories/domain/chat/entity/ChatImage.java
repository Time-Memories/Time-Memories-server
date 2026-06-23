package com.example.memories.domain.chat.entity;

import com.example.memories.global.common.entity.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "chat_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatImage extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Chat chat;

    @Column(name = "image_key", nullable = false)
    private String imageKey;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder
    public ChatImage(Chat chat, String imageKey, Integer sortOrder) {
        this.chat = chat;
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
    }
}
