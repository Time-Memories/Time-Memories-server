package com.example.memories.domain.user.controller;

import com.example.memories.domain.user.dto.request.UpdateUserRequestDto;
import com.example.memories.domain.user.dto.response.UpdateUserResponseDto;
import com.example.memories.domain.user.dto.response.UserProfileResponseDto;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.domain.user.entity.User;
import com.example.memories.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserService userService;

    @InjectMocks UserController userController;

    private User buildUser(Long id) {
        User user = User.builder()
                .name("Test User").email("test@example.com")
                .provider(AuthProvider.GOOGLE).providerId("google-id").build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("내 정보 조회 시 200 OK와 프로필 DTO를 반환한다")
    void getMyProfile_returnsOk() {
        User user = buildUser(1L);
        UserProfileResponseDto dto = new UserProfileResponseDto(
                1L, "test@example.com", "Test User", LocalDateTime.of(2026, 5, 1, 0, 0));
        given(userService.getMyProfile(user)).willReturn(dto);

        ResponseEntity<UserProfileResponseDto> response = userController.getMyProfile(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("유저 정보 수정 시 200 OK와 수정된 DTO를 반환한다")
    void updateMyProfile_returnsOk() {
        User user = buildUser(1L);
        UpdateUserRequestDto request = new UpdateUserRequestDto("New Name");
        UpdateUserResponseDto dto = new UpdateUserResponseDto(1L, "New Name");
        given(userService.updateMyProfile(1L, request)).willReturn(dto);

        ResponseEntity<UpdateUserResponseDto> response = userController.updateMyProfile(user, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("회원 탈퇴 시 204 No Content를 반환하고 서비스를 호출한다")
    void deleteUser_returnsNoContent() {
        User user = buildUser(1L);

        ResponseEntity<Void> response = userController.deleteUser(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        then(userService).should().deleteUser(1L);
    }
}
