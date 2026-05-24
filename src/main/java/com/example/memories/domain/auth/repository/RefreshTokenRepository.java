package com.example.memories.domain.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository {
    Optional<String> findByUserId(Long userId);
    void save(Long userId, String token, LocalDateTime expiresAt);
    void delete(Long userId);
}
