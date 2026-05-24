package com.example.memories.domain.auth.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRedisRepositoryTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    @InjectMocks RefreshTokenRedisRepository repository;

    @Test
    @DisplayName("토큰 저장 시 올바른 키와 TTL로 Redis에 저장한다")
    void save_storesTokenWithTtl() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        repository.save(1L, "token-value", expiresAt);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        then(valueOperations).should().set(eq("refresh:1"), eq("token-value"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue().getSeconds()).isGreaterThan(3500L);
    }

    @Test
    @DisplayName("만료된 expiresAt으로 저장 시 Redis에 저장하지 않는다")
    void save_expiredExpiresAt_doesNotStore() {
        LocalDateTime expiredAt = LocalDateTime.now().minusHours(1);

        repository.save(1L, "token-value", expiredAt);

        then(redisTemplate).should(never()).opsForValue();
    }

    @Test
    @DisplayName("저장된 토큰을 userId로 조회하면 반환한다")
    void findByUserId_returnsStoredToken() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:1")).willReturn("stored-token");

        Optional<String> result = repository.findByUserId(1L);

        assertThat(result).contains("stored-token");
    }

    @Test
    @DisplayName("토큰이 없으면 빈 Optional을 반환한다")
    void findByUserId_notFound_returnsEmpty() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:99")).willReturn(null);

        Optional<String> result = repository.findByUserId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("delete 호출 시 올바른 키로 Redis에서 삭제한다")
    void delete_removesTokenByKey() {
        repository.delete(1L);

        then(redisTemplate).should().delete("refresh:1");
    }
}
