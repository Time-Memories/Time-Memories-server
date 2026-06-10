package com.example.memories.infra.oauth.kakao;

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

class KakaoOAuthClientTest {

    private MockRestServiceServer server;
    private KakaoOAuthClient kakaoOAuthClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        kakaoOAuthClient = new KakaoOAuthClient(
                builder,
                "kakao-client-id",
                "kakao-client-secret",
                "https://app.example.com/callback"
        );
    }

    @Test
    @DisplayName("인가 코드를 access token으로 교환한다")
    void getAccessToken_success() {
        String responseJson = """
                {
                    "access_token": "kakao-access-token",
                    "token_type": "bearer"
                }
                """;
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(formWith(
                        "grant_type", "authorization_code",
                        "client_id", "kakao-client-id",
                        "redirect_uri", "https://app.example.com/callback",
                        "code", "auth-code",
                        "client_secret", "kakao-client-secret")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String accessToken = kakaoOAuthClient.getAccessToken("auth-code");

        assertThat(accessToken).isEqualTo("kakao-access-token");
    }

    @Test
    @DisplayName("토큰 교환 실패 시 OAUTH_COMMUNICATION_ERROR 예외 발생")
    void getAccessToken_serverError_throwsOAuthCommunicationError() {
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> kakaoOAuthClient.getAccessToken("bad-code"))
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
    @DisplayName("Kakao userinfo API 응답을 OAuthUserInfo로 정확히 매핑한다")
    void getUserInfo_success() {
        String responseJson = """
                {
                    "id": 9876543,
                    "kakao_account": {
                        "email": "user@kakao.com",
                        "profile": {
                            "nickname": "Kakao User"
                        }
                    }
                }
                """;
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(header("Authorization", "Bearer valid-token"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        OAuthUserInfo result = kakaoOAuthClient.getUserInfo("valid-token");

        assertThat(result.providerId()).isEqualTo("9876543");
        assertThat(result.email()).isEqualTo("user@kakao.com");
        assertThat(result.name()).isEqualTo("Kakao User");
        assertThat(result.provider()).isEqualTo(AuthProvider.KAKAO);
    }

    @Test
    @DisplayName("kakao_account 정보가 없으면 null과 'unknown'으로 매핑한다")
    void getUserInfo_missingAccountFields_usesDefaults() {
        String responseJson = """
                {
                    "id": 1111111,
                    "kakao_account": {}
                }
                """;
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        OAuthUserInfo result = kakaoOAuthClient.getUserInfo("valid-token");

        assertThat(result.providerId()).isEqualTo("1111111");
        assertThat(result.email()).isNull();
        assertThat(result.name()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Kakao 서버 오류 시 OAUTH_COMMUNICATION_ERROR 예외 발생")
    void getUserInfo_serverError_throwsOAuthCommunicationError() {
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> kakaoOAuthClient.getUserInfo("invalid-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.OAUTH_COMMUNICATION_ERROR));
    }
}
