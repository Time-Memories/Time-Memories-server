package com.example.memories.domain.comment.service;

import com.example.memories.domain.comment.dto.request.CommentCreateRequestDto;
import com.example.memories.domain.comment.dto.request.CommentUpdateRequestDto;
import com.example.memories.domain.comment.dto.response.CommentListResponseDto;
import com.example.memories.domain.comment.dto.response.CommentResponseDto;
import com.example.memories.domain.comment.entity.Comment;
import com.example.memories.domain.comment.exception.CommentErrorCode;
import com.example.memories.domain.comment.repository.CommentRepository;
import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.diary.exception.DiaryErrorCode;
import com.example.memories.domain.diary.repository.DiaryRepository;
import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.repository.RoomUserRepository;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final DiaryRepository diaryRepository;
    private final RoomUserRepository roomUserRepository;

    @Transactional
    @Override
    public CommentResponseDto createComment(User user, Long diaryId, CommentCreateRequestDto request) {
        Diary diary = findDiaryOrThrow(diaryId);
        validateRoomMember(diary.getRoom(), user);

        Comment comment = Comment.builder()
                .diary(diary)
                .user(user)
                .content(request.content())
                .build();
        commentRepository.save(comment);

        return CommentResponseDto.from(comment);
    }

    @Override
    public CommentListResponseDto getComments(User user, Long diaryId, Long cursor, int size) {
        Diary diary = findDiaryOrThrow(diaryId);
        validateRoomMember(diary.getRoom(), user);

        List<Comment> fetched = commentRepository.findByDiaryCursor(diary, cursor, PageRequest.of(0, size + 1));

        boolean hasNext = fetched.size() > size;
        List<Comment> content = hasNext ? fetched.subList(0, size) : fetched;

        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        List<CommentListResponseDto.CommentItem> items = content.stream()
                .map(CommentListResponseDto.CommentItem::from)
                .toList();

        return CommentListResponseDto.of(items, nextCursor, hasNext);
    }

    @Transactional
    @Override
    public CommentResponseDto updateComment(User user, Long commentId, CommentUpdateRequestDto request) {
        Comment comment = findCommentWithDiaryRoomOrThrow(commentId);
        validateAuthorAndStillMember(comment, user);

        comment.update(request.content());

        return CommentResponseDto.from(comment);
    }

    @Transactional
    @Override
    public void deleteComment(User user, Long commentId) {
        Comment comment = findCommentWithDiaryRoomOrThrow(commentId);
        validateAuthorAndStillMember(comment, user);

        commentRepository.delete(comment);
    }

    private Diary findDiaryOrThrow(Long diaryId) {
        // 권한 검사 시 diary.getRoom() 프록시 초기화로 추가 쿼리가 발생하지 않도록 Room을 함께 조회
        return diaryRepository.findWithRoomById(diaryId)
                .orElseThrow(() -> new BusinessException(DiaryErrorCode.DIARY_NOT_FOUND));
    }

    private Comment findCommentWithDiaryRoomOrThrow(Long commentId) {
        return commentRepository.findWithDiaryRoomById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateRoomMember(Room room, User user) {
        if (!roomUserRepository.existsByRoomAndUser(room, user)) {
            throw new BusinessException(CommentErrorCode.COMMENT_FORBIDDEN);
        }
    }

    private void validateAuthorAndStillMember(Comment comment, User user) {
        if (!comment.isAuthor(user.getId())) {
            throw new BusinessException(CommentErrorCode.COMMENT_NOT_AUTHOR);
        }
        if (!roomUserRepository.existsByRoomAndUser(comment.getDiary().getRoom(), user)) {
            throw new BusinessException(CommentErrorCode.COMMENT_AUTHOR_LEFT_ROOM);
        }
    }
}
