package com.example.memories.infra.oauth;

import com.example.memories.domain.user.entity.AuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthClientCompositeTest {

    @Mock OAuthClient googleClient;
    @Mock OAuthClient kakaoClient;

    private OAuthClientComposite composite;

    @BeforeEach
    void setUp() {
        given(googleClient.getProvider()).willReturn(AuthProvider.GOOGLE);
        given(kakaoClient.getProvider()).willReturn(AuthProvider.KAKAO);
        composite = new OAuthClientComposite(List.of(googleClient, kakaoClient));
    }

    @Test
    @DisplayName("GOOGLE provider로 조회하면 GoogleOAuthClient를 반환한다")
    void getClient_google_returnsGoogleClient() {
        OAuthClient result = composite.getClient(AuthProvider.GOOGLE);

        assertThat(result).isSameAs(googleClient);
    }

    @Test
    @DisplayName("KAKAO provider로 조회하면 KakaoOAuthClient를 반환한다")
    void getClient_kakao_returnsKakaoClient() {
        OAuthClient result = composite.getClient(AuthProvider.KAKAO);

        assertThat(result).isSameAs(kakaoClient);
    }
}
