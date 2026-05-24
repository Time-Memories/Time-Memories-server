package com.example.memories.domain.user.service;

import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.domain.user.entity.User;

public interface UserService {
    User findOrRegisterOAuthUser(AuthProvider provider, String providerId, String name, String email);
    User findById(Long userId);
}
