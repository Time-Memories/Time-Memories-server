package com.example.memories.domain.diary.entity;

import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.common.entity.AuditingEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contents;

    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<DiaryImage> images = new ArrayList<>();

    @Builder
    public Diary(User user, Room room, String title, String contents, LocalDate diaryDate) {
        this.user = user;
        this.room = room;
        this.title = title;
        this.contents = contents;
        this.diaryDate = diaryDate;
    }

    public void update(String title, String contents, LocalDate diaryDate) {
        if (title != null) this.title = title;
        if (contents != null) this.contents = contents;
        if (diaryDate != null) this.diaryDate = diaryDate;
    }

    public void updateImages(List<String> newImageKeys) {
        this.images.clear();
        if (newImageKeys != null) {
            for (int i = 0; i < newImageKeys.size(); i++) {
                this.images.add(DiaryImage.builder()
                        .diary(this)
                        .imageKey(newImageKeys.get(i))
                        .sortOrder(i)
                        .build());
            }
        }
    }

    public boolean isAuthor(Long userId) {
        return this.user.getId().equals(userId);
    }
}