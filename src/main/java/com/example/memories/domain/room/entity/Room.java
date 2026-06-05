package com.example.memories.domain.room.entity;

import com.example.memories.domain.room.entity.enums.RoomType;
import com.example.memories.global.common.entity.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType type;

    @Column(nullable = false)
    private String title;

    @Column(name = "room_code", nullable = false, unique = true)
    private String roomCode;

    @Builder
    public Room(RoomType type, String title, String roomCode) {
        this.type = type;
        this.title = title;
        this.roomCode = roomCode;
    }

    public void update(String title) {
        if (title != null) {
            this.title = title;
        }
    }
}
