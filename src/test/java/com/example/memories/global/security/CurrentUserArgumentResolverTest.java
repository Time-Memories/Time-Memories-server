package com.example.memories.global.security;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.domain.user.entity.User;
import com.example.memories.domain.user.service.UserService;
import com.example.memories.global.annotation.CurrentUser;
import com.example.memories.global.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserArgumentResolverTest {

    @Mock UserService userService;

    @InjectMocks CurrentUserArgumentResolver resolver;

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("@CurrentUser User 파라미터는 지원한다")
    void supportsParameter_currentUserAnnotationWithUserType_returnsTrue() throws NoSuchMethodException {
        MethodParameter parameter = methodParameter(TestController.class, "annotated", User.class);

        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @Test
    @DisplayName("@CurrentUser가 없는 파라미터는 지원하지 않는다")
    void supportsParameter_withoutAnnotation_returnsFalse() throws NoSuchMethodException {
        MethodParameter parameter = methodParameter(TestController.class, "noAnnotation", User.class);

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @Test
    @DisplayName("@CurrentUser가 있지만 타입이 User가 아니면 지원하지 않는다")
    void supportsParameter_wrongType_returnsFalse() throws NoSuchMethodException {
        MethodParameter parameter = methodParameter(TestController.class, "wrongType", Long.class);

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @Test
    @DisplayName("인증된 사용자의 User 객체를 반환한다")
    void resolveArgument_authenticated_returnsUser() {
        User user = User.builder()
                .name("Test").email("test@example.com")
                .provider(AuthProvider.GOOGLE).providerId("g-id").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()));
        given(userService.findById(1L)).willReturn(user);

        User result = (User) resolver.resolveArgument(null, null, null, null);

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("인증 정보가 없으면 AUTHENTICATION_REQUIRED 예외 발생")
    void resolveArgument_noAuthentication_throwsAuthenticationRequired() {
        assertThatThrownBy(() -> resolver.resolveArgument(null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.AUTHENTICATION_REQUIRED));
    }

    private MethodParameter methodParameter(Class<?> clazz, String methodName, Class<?>... paramTypes)
            throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(methodName, paramTypes);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    static class TestController {
        public void annotated(@CurrentUser User user) {}
        public void noAnnotation(User user) {}
        public void wrongType(@CurrentUser Long id) {}
    }
}
