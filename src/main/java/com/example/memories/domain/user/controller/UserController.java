package com.example.memories.domain.user.controller;

import com.example.memories.domain.user.dto.request.UpdateUserRequestDto;
import com.example.memories.domain.user.dto.response.UpdateUserResponseDto;
import com.example.memories.domain.user.dto.response.UserProfileResponseDto;
import com.example.memories.domain.user.entity.User;
import com.example.memories.domain.user.service.UserService;
import com.example.memories.global.annotation.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "유저 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "나의 프로필 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponseDto> getMyProfile(
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(userService.getMyProfile(user));
    }

    @Operation(summary = "유저 정보 수정", description = "내 정보를 수정합니다.")
    @PatchMapping("/me")
    public ResponseEntity<UpdateUserResponseDto> updateMyProfile(
            @CurrentUser User user,
            @RequestBody @Valid UpdateUserRequestDto request) {
        return ResponseEntity.ok(userService.updateMyProfile(user.getId(), request));
    }

    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 진행합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(
            @CurrentUser User user
    ) {
        userService.deleteUser(user.getId());
        return ResponseEntity.noContent().build();
    }
}
