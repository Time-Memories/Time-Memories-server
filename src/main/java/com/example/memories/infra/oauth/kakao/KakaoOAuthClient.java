package com.example.memories.infra.oauth.kakao;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.infra.oauth.OAuthClient;
import com.example.memories.infra.oauth.OAuthUserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoOAuthClient implements OAuthClient {

    private final RestClient restClient;

    public KakaoOAuthClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://kapi.kakao.com")
                .build();
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        try {
            JsonNode response = restClient.get()
                    .uri("/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode account = response.path("kakao_account");
            String email = account.path("email").asText(null);
            String name = account.path("profile").path("nickname").asText("unknown");

            return new OAuthUserInfo(
                    response.get("id").asText(),
                    email,
                    name,
                    AuthProvider.KAKAO
            );
        } catch (RestClientException e) {
            throw new BusinessException(AuthErrorCode.OAUTH_COMMUNICATION_ERROR);
        }
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.KAKAO;
    }
}
