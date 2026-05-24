package com.example.memories.infra.oauth.kakao;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.infra.oauth.OAuthUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
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
        kakaoOAuthClient = new KakaoOAuthClient(builder);
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
