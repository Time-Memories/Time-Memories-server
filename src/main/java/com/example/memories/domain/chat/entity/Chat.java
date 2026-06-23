package com.example.memories.domain.chat.entity;

import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.common.entity.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "chat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Chat extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder
    public Chat(Room room, User user, String content) {
        this.room = room;
        this.user = user;
        this.content = content;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public String getUserName() {
        return user != null ? user.getName() : "탈퇴한 사용자";
    }
}
