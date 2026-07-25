package com.carlosalbertoxw.notes.auth;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** Pruebas unitarias del resolver que inyecta el key_id del cliente autenticado. */
class CurrentKeyIdArgumentResolverTest {

    private final CurrentKeyIdArgumentResolver resolver = new CurrentKeyIdArgumentResolver();

    @SuppressWarnings("unused")
    private void handler(@CurrentKeyId String keyId, @CurrentKeyId Integer wrongType, String plain) {
    }

    private static MethodParameter parameter(int index) throws NoSuchMethodException {
        Method method = CurrentKeyIdArgumentResolverTest.class
                .getDeclaredMethod("handler", String.class, Integer.class, String.class);
        return new MethodParameter(method, index);
    }

    @Test
    void supportsOnlyAnnotatedStringParameters() throws Exception {
        assertThat(resolver.supportsParameter(parameter(0))).isTrue();
        assertThat(resolver.supportsParameter(parameter(1))).as("anotado pero no String").isFalse();
        assertThat(resolver.supportsParameter(parameter(2))).as("String sin anotar").isFalse();
    }

    @Test
    void resolvesTheKeyIdLeftByTheAuthFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notes");
        request.setAttribute(AuthenticatedClient.REQUEST_ATTRIBUTE,
                new AuthenticatedClient("cliente-1", "Cliente 1"));
        NativeWebRequest webRequest = new ServletWebRequest(request);

        assertThat(resolver.resolveArgument(parameter(0), null, webRequest, null))
                .isEqualTo("cliente-1");
    }

    @Test
    void failsFastWhenThereIsNoAuthenticatedClient() throws Exception {
        NativeWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/notes"));

        assertThatIllegalStateException()
                .isThrownBy(() -> resolver.resolveArgument(parameter(0), null, webRequest, null));
    }
}
