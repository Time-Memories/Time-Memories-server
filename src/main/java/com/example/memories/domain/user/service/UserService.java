package com.example.memories.domain.user.service;

import com.example.memories.domain.user.dto.request.UpdateUserRequestDto;
import com.example.memories.domain.user.dto.response.UpdateUserResponseDto;
import com.example.memories.domain.user.dto.response.UserProfileResponseDto;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.domain.user.entity.User;

public interface UserService {
    User findOrRegisterOAuthUser(AuthProvider provider, String providerId, String name, String email);
    User findById(Long userId);
    UserProfileResponseDto getMyProfile(User user);
    UpdateUserResponseDto updateMyProfile(Long userId, UpdateUserRequestDto request);
    void deleteUser(Long userId);
}
