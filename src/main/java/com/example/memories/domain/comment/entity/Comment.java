package com.example.memories.domain.comment.entity;

import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.common.entity.AuditingEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends AuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    // 작성자가 탈퇴(soft delete)해도 댓글은 유지하기 위해, 조회에서 제외된 작성자는 user=null로 로딩한다.
    // (@NotFound가 없으면 Hibernate가 FetchNotFoundException을 던짐)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public Comment(Diary diary, User user, String content) {
        this.diary = diary;
        this.user = user;
        this.content = content;
    }

    public void update(String content) {
        // 컨트롤러의 @NotBlank 1차 방어와 별개로 엔티티 스스로 무결성 보장 (null/공백 무시)
        if (StringUtils.hasText(content)) {
            this.content = content;
        }
    }

    public boolean isAuthor(Long userId) {
        return this.user.getId().equals(userId);
    }
}
