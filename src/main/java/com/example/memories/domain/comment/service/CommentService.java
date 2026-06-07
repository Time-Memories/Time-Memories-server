package com.example.memories.domain.comment.service;

import com.example.memories.domain.comment.dto.request.CommentCreateRequestDto;
import com.example.memories.domain.comment.dto.request.CommentUpdateRequestDto;
import com.example.memories.domain.comment.dto.response.CommentListResponseDto;
import com.example.memories.domain.comment.dto.response.CommentResponseDto;
import com.example.memories.domain.user.entity.User;

public interface CommentService {

    CommentResponseDto createComment(User user, Long diaryId, CommentCreateRequestDto request);

    CommentListResponseDto getComments(User user, Long diaryId, Long cursor, int size);

    CommentResponseDto updateComment(User user, Long commentId, CommentUpdateRequestDto request);

    void deleteComment(User user, Long commentId);
}
