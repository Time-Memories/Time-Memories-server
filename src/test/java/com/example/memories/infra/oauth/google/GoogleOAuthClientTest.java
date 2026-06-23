package com.example.memories.infra.oauth.google;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.infra.oauth.OAuthUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GoogleOAuthClientTest {

    private MockRestServiceServer server;
    private GoogleOAuthClient googleOAuthClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        googleOAuthClient = new GoogleOAuthClient(
                builder,
                "google-client-id",
                "google-client-secret",
                "https://app.example.com/callback"
        );
    }

    @Test
    @DisplayName("authorize URL에 client_id, redirect_uri, scope, state가 포함된다")
    void getAuthorizationUri_containsParams() {
        String uri = googleOAuthClient.getAuthorizationUri("google:nonce-1");

        assertThat(uri).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(uri).contains("client_id=google-client-id");
        assertThat(uri).contains("response_type=code");
        assertThat(uri).contains("scope=email%20profile");
        assertThat(uri).contains("state=google:nonce-1");
    }

    @Test
    @DisplayName("인가 코드를 access token으로 교환한다")
    void getAccessToken_success() {
        String responseJson = """
                {
                    "access_token": "google-access-token",
                    "token_type": "Bearer"
                }
                """;
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(formWith(
                        "grant_type", "authorization_code",
                        "client_id", "google-client-id",
                        "client_secret", "google-client-secret",
                        "redirect_uri", "https://app.example.com/callback",
                        "code", "auth-code")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String accessToken = googleOAuthClient.getAccessToken("auth-code");

        assertThat(accessToken).isEqualTo("google-access-token");
    }

    @Test
    @DisplayName("토큰 교환 실패 시 OAUTH_COMMUNICATION_ERROR 예외 발생")
    void getAccessToken_serverError_throwsOAuthCommunicationError() {
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> googleOAuthClient.getAccessToken("bad-code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.OAUTH_COMMUNICATION_ERROR));
    }

    private static MultiValueMap<String, String> formWith(String... keyValues) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            form.add(keyValues[i], keyValues[i + 1]);
        }
        return form;
    }

    @Test
    @DisplayName("Google userinfo API 응답을 OAuthUserInfo로 정확히 매핑한다")
    void getUserInfo_success() {
        String responseJson = """
                {
                    "sub": "google-sub-123",
                    "email": "user@gmail.com",
                    "name": "Google User"
                }
                """;
        server.expect(requestTo("https://www.googleapis.com/oauth2/v3/userinfo"))
                .andExpect(header("Authorization", "Bearer valid-token"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        OAuthUserInfo result = googleOAuthClient.getUserInfo("valid-token");

        assertThat(result.providerId()).isEqualTo("google-sub-123");
        assertThat(result.email()).isEqualTo("user@gmail.com");
        assertThat(result.name()).isEqualTo("Google User");
        assertThat(result.provider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    @DisplayName("email, name 필드가 없으면 null과 'unknown'으로 매핑한다")
    void getUserInfo_missingOptionalFields_usesDefaults() {
        String responseJson = """
                {
                    "sub": "google-sub-456"
                }
                """;
        server.expect(requestTo("https://www.googleapis.com/oauth2/v3/userinfo"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        OAuthUserInfo result = googleOAuthClient.getUserInfo("valid-token");

        assertThat(result.providerId()).isEqualTo("google-sub-456");
        assertThat(result.email()).isNull();
        assertThat(result.name()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Google 서버 오류 시 OAUTH_COMMUNICATION_ERROR 예외 발생")
    void getUserInfo_serverError_throwsOAuthCommunicationError() {
        server.expect(requestTo("https://www.googleapis.com/oauth2/v3/userinfo"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> googleOAuthClient.getUserInfo("invalid-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.OAUTH_COMMUNICATION_ERROR));
    }
}
