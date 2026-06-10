package com.example.memories.infra.oauth.kakao;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.infra.oauth.OAuthClient;
import com.example.memories.infra.oauth.OAuthUserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoOAuthClient implements OAuthClient {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoOAuthClient(
            RestClient.Builder builder,
            @Value("${oauth.kakao.client-id}") String clientId,
            @Value("${oauth.kakao.client-secret}") String clientSecret,
            @Value("${oauth.kakao.redirect-uri}") String redirectUri) {
        this.restClient = builder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public String getAccessToken(String authorizationCode) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", clientId);
            form.add("redirect_uri", redirectUri);
            form.add("code", authorizationCode);
            if (clientSecret != null && !clientSecret.isBlank()) {
                form.add("client_secret", clientSecret);
            }

            JsonNode response = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);

            return response.get("access_token").asText();
        } catch (RestClientException e) {
            throw new BusinessException(AuthErrorCode.OAUTH_COMMUNICATION_ERROR);
        }
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        try {
            JsonNode response = restClient.get()
                    .uri(USER_INFO_URI)
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
