package com.example.memories.domain.room.entity;

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

    private String type;

    private String title;

    @Column(name = "room_code", nullable = false, unique = true)
    private String roomCode;

    @Builder
    public Room(String type, String title, String roomCode) {
        this.type = type;
        this.title = title;
        this.roomCode = roomCode;
    }

    public void update(String title, String type) {
        if (title != null) {
            this.title = title;
        }

        if (type != null) {
            this.type = type;
        }
    }

    public void updateRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

}
