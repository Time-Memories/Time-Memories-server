package com.example.memories.domain.room.entity;

import com.example.memories.domain.room.entity.enums.RoomRole;
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
@Table(
        name = "room_user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_room_user",
                        columnNames = {"room_id", "user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomUser extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomRole role;

    @Builder
    public RoomUser(Room room, User user, RoomRole role) {
        this.room = room;
        this.user = user;
        this.role = role;
    }

    public boolean isOwner() {
        return this.role == RoomRole.OWNER;
    }

    public void changeRole(RoomRole role) {
        this.role = role;
    }
}
