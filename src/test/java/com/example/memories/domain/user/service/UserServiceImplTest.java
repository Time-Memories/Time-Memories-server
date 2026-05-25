package com.example.memories.domain.user.service;

import com.example.memories.domain.auth.repository.RefreshTokenRepository;
import com.example.memories.domain.user.dto.request.UpdateUserRequestDto;
import com.example.memories.domain.user.dto.response.UpdateUserResponseDto;
import com.example.memories.domain.user.dto.response.UserProfileResponseDto;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.domain.user.entity.User;
import com.example.memories.domain.user.exception.UserErrorCode;
import com.example.memories.domain.user.repository.UserRepository;
import com.example.memories.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks UserServiceImpl userService;

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("존재하는 userId로 조회하면 User를 반환한다")
    void findById_success() {
        User user = User.builder()
                .name("Test User").email("test@example.com")
                .provider(AuthProvider.GOOGLE).providerId("google-id").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 userId로 조회하면 USER_NOT_FOUND 예외 발생")
    void findById_notFound_throwsUserNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    // ── findOrRegisterOAuthUser ───────────────────────────────────────────────

    @Test
    @DisplayName("신규 소셜 유저는 DB에 저장 후 반환한다")
    void findOrRegisterOAuthUser_newUser_savesAndReturns() {
        User saved = User.builder()
                .name("New User").email("new@example.com")
                .provider(AuthProvider.KAKAO).providerId("kakao-id").build();
        given(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "kakao-id"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(saved);

        User result = userService.findOrRegisterOAuthUser(AuthProvider.KAKAO, "kakao-id", "New User", "new@example.com");

        assertThat(result.getName()).isEqualTo("New User");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("기존 소셜 유저는 사용자가 설정한 이름을 그대로 유지하며 반환한다")
    void findOrRegisterOAuthUser_existingUser_preservesName() {
        User existing = User.builder()
                .name("사용자가 바꾼 이름").email("user@example.com")
                .provider(AuthProvider.GOOGLE).providerId("google-id").build();
        given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-id"))
                .willReturn(Optional.of(existing));

        User result = userService.findOrRegisterOAuthUser(AuthProvider.GOOGLE, "google-id", "소셜 계정 이름", "user@example.com");

        assertThat(result.getName()).isEqualTo("사용자가 바꾼 이름");
        then(userRepository).should(never()).save(any());
    }

    // ── getMyProfile ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("User 엔티티를 받아 UserProfileResponseDto로 변환한다")
    void getMyProfile_success() {
        User user = User.builder()
                .name("Test User").email("test@example.com")
                .provider(AuthProvider.GOOGLE).providerId("google-id").build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UserProfileResponseDto result = userService.getMyProfile(user);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("Test User");
        then(userRepository).shouldHaveNoInteractions();
    }

    // ── updateMyProfile ───────────────────────────────────────────────────────

    @Test
    @DisplayName("유저 이름 수정 시 변경된 이름으로 UpdateUserResponseDto를 반환한다")
    void updateMyProfile_success() {
        User user = User.builder()
                .name("Old Name").email("test@example.com")
                .provider(AuthProvider.GOOGLE).providerId("google-id").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UpdateUserResponseDto result = userService.updateMyProfile(1L, new UpdateUserRequestDto("New Name"));

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("New Name");
        assertThat(user.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("존재하지 않는 userId로 정보 수정 시 USER_NOT_FOUND 예외 발생")
    void updateMyProfile_notFound_throwsUserNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMyProfile(999L, new UpdateUserRequestDto("Name")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("회원 탈퇴 시 유저를 삭제하고 Refresh Token도 함께 삭제한다")
    void deleteUser_success() {
        User user = User.builder()
                .name("Test User").email("test@example.com")
                .provider(AuthProvider.GOOGLE).providerId("google-id").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.deleteUser(1L);

        then(userRepository).should().delete(user);
        then(refreshTokenRepository).should().delete(1L);
    }

    @Test
    @DisplayName("존재하지 않는 userId로 회원 탈퇴 시 USER_NOT_FOUND 예외 발생")
    void deleteUser_notFound_throwsUserNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));

        then(userRepository).should(never()).delete(any(User.class));
        then(refreshTokenRepository).should(never()).delete(any());
    }
}
