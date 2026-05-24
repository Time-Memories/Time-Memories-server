package com.example.memories.domain.user.service;

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

    @InjectMocks UserServiceImpl userService;

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
    @DisplayName("기존 소셜 유저는 이름을 갱신하고 반환한다")
    void findOrRegisterOAuthUser_existingUser_updatesName() {
        User existing = User.builder()
                .name("Old Name").email("user@example.com")
                .provider(AuthProvider.GOOGLE).providerId("google-id").build();
        given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-id"))
                .willReturn(Optional.of(existing));

        User result = userService.findOrRegisterOAuthUser(AuthProvider.GOOGLE, "google-id", "New Name", "user@example.com");

        assertThat(result.getName()).isEqualTo("New Name");
        then(userRepository).should(never()).save(any());
    }
}
