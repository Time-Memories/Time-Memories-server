package com.example.memories.domain.comment.controller;

import com.example.memories.domain.comment.dto.request.CommentCreateRequestDto;
import com.example.memories.domain.comment.dto.request.CommentUpdateRequestDto;
import com.example.memories.domain.comment.dto.response.CommentListResponseDto;
import com.example.memories.domain.comment.dto.response.CommentResponseDto;
import com.example.memories.domain.comment.service.CommentService;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.annotation.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 API")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 생성", description = "일기에 댓글을 작성합니다. 해당 일기가 속한 방의 멤버만 작성 가능합니다.")
    @PostMapping("/api/diaries/{diaryId}/comments")
    public ResponseEntity<CommentResponseDto> createComment(
            @CurrentUser User user,
            @PathVariable Long diaryId,
            @RequestBody @Valid CommentCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(user, diaryId, request));
    }

    @Operation(summary = "댓글 조회", description = "일기에 달린 댓글을 작성 시간 순으로 커서 기반 조회합니다. 해당 일기가 속한 방의 멤버만 조회 가능합니다.")
    @GetMapping("/api/diaries/{diaryId}/comments")
    public ResponseEntity<CommentListResponseDto> getComments(
            @CurrentUser User user,
            @PathVariable Long diaryId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(commentService.getComments(user, diaryId, cursor, size));
    }

    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글을 수정합니다. 방을 탈퇴한 경우 수정할 수 없습니다.")
    @PatchMapping("/api/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @CurrentUser User user,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentUpdateRequestDto request
    ) {
        return ResponseEntity.ok(commentService.updateComment(user, commentId, request));
    }

    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글을 삭제합니다. 방을 탈퇴한 경우 삭제할 수 없습니다.")
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @CurrentUser User user,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(user, commentId);
        return ResponseEntity.noContent().build();
    }
}
