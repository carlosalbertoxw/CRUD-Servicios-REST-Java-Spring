package com.carlosalbertoxw.notes.auth;

import com.carlosalbertoxw.notes.support.TestProblems;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del filtro de autenticacion: se ejercita con dobles de
 * prueba del repositorio, sin contexto de Spring ni base de datos.
 */
class ApiKeyAuthFilterTest {

    private static final String KEY_ID = "cliente-1";
    private static final String SECRET = "secreto-correcto";

    private final FakeApiKeyRepository keys = new FakeApiKeyRepository();
    private final ApiKeyAuthFilter filter = new ApiKeyAuthFilter(keys, TestProblems.responder());

    private static MockHttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }

    private static byte[] sha256(String value) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void letsPublicPathsThroughWithoutKey() throws Exception {
        for (String path : List.of("/", "/favicon.ico", "/actuator/health/readiness",
                "/v3/api-docs", "/swagger-ui/index.html")) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request(path), response, chain);

            assertThat(chain.getRequest()).as("ruta publica %s", path).isNotNull();
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void rejectsMissingHeaderWithChallenge() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/notes"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo(ApiKeyAuthFilter.SCHEME_NAME);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsHeaderWithoutKeyIdSecretSeparator() throws Exception {
        for (String header : List.of("sin-separador", ".solo-secreto", "solo-id.", "   ")) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = request("/api/notes");
            request.addHeader(ApiKeyAuthFilter.HEADER_NAME, header);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).as("header '%s'", header).isEqualTo(401);
            assertThat(chain.getRequest()).isNull();
        }
    }

    @Test
    void rejectsUnknownKeyId() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = request("/api/notes");
        request.addHeader(ApiKeyAuthFilter.HEADER_NAME, "no-existe." + SECRET);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsWrongSecretForExistingKey() throws Exception {
        keys.store(new ApiKey(KEY_ID, sha256(SECRET), "Cliente 1"));
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = request("/api/notes");
        request.addHeader(ApiKeyAuthFilter.HEADER_NAME, KEY_ID + ".secreto-incorrecto");

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
        assertThat(keys.registeredUses).isEmpty();
    }

    @Test
    void authenticatesValidKeyAndPublishesClientIdentity() throws Exception {
        keys.store(new ApiKey(KEY_ID, sha256(SECRET), "Cliente 1"));
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = request("/api/notes");
        request.addHeader(ApiKeyAuthFilter.HEADER_NAME, KEY_ID + "." + SECRET);

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute(AuthenticatedClient.REQUEST_ATTRIBUTE))
                .isEqualTo(new AuthenticatedClient(KEY_ID, "Cliente 1"));
        assertThat(keys.registeredUses).containsExactly(KEY_ID);
    }

    @Test
    void secretMayContainDots() throws Exception {
        String dottedSecret = "parte1.parte2.parte3";
        keys.store(new ApiKey(KEY_ID, sha256(dottedSecret), "Cliente 1"));
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletRequest request = request("/api/notes");
        request.addHeader(ApiKeyAuthFilter.HEADER_NAME, KEY_ID + "." + dottedSecret);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void failureRegisteringUseDoesNotBreakAuthentication() throws Exception {
        keys.store(new ApiKey(KEY_ID, sha256(SECRET), "Cliente 1"));
        keys.failOnRegisterUse = true;
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = request("/api/notes");
        request.addHeader(ApiKeyAuthFilter.HEADER_NAME, KEY_ID + "." + SECRET);

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    /** Repositorio en memoria que ademas registra los usos anotados. */
    private static final class FakeApiKeyRepository implements ApiKeyRepository {

        private ApiKey stored;
        private boolean failOnRegisterUse;
        private final List<String> registeredUses = new ArrayList<>();

        void store(ApiKey key) {
            this.stored = key;
        }

        @Override
        public Optional<ApiKey> getActiveKey(String keyId) {
            return Optional.ofNullable(stored).filter(key -> key.keyId().equals(keyId));
        }

        @Override
        public void registerUse(String keyId) {
            if (failOnRegisterUse) {
                throw new IllegalStateException("BD caida");
            }
            registeredUses.add(keyId);
        }
    }
}
