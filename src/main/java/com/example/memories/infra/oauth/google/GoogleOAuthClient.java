package com.example.memories.infra.oauth.google;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.infra.oauth.OAuthClient;
import com.example.memories.infra.oauth.OAuthUserInfo;
import tools.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GoogleOAuthClient implements OAuthClient {

    private final RestClient restClient;

    public GoogleOAuthClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://www.googleapis.com")
                .build();
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        try {
            JsonNode response = restClient.get()
                    .uri("/oauth2/v3/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);

            return new OAuthUserInfo(
                    response.get("sub").asText(),
                    response.path("email").asText(null),
                    response.path("name").asText("unknown"),
                    AuthProvider.GOOGLE
            );
        } catch (RestClientException e) {
            throw new BusinessException(AuthErrorCode.OAUTH_COMMUNICATION_ERROR);
        }
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.GOOGLE;
    }
}