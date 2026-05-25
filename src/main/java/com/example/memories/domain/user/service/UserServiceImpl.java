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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new BusinessException(UserErrorCode.USER_NOT_FOUND)
                );
    }

    @Transactional
    @Override
    public User findOrRegisterOAuthUser(AuthProvider provider, String providerId, String name, String email) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .provider(provider)
                        .providerId(providerId)
                        .name(name)
                        .email(email)
                        .build()));
    }

    @Override
    public UserProfileResponseDto getMyProfile(User user) {

        return UserProfileResponseDto.from(user);
    }

    @Transactional
    @Override
    public UpdateUserResponseDto updateMyProfile(Long userId, UpdateUserRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        user.updateName(request.name());
        return UpdateUserResponseDto.from(user);
    }

    @Transactional
    @Override
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
        refreshTokenRepository.delete(userId);
    }
}
