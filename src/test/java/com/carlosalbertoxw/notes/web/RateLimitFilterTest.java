package com.carlosalbertoxw.notes.web;

import com.carlosalbertoxw.notes.support.TestProblems;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas unitarias del limitador de peticiones por IP (ventana fija). */
class RateLimitFilterTest {

    private static MockHttpServletRequest requestFrom(String ip) {
        // Cada peticion debe ser un objeto nuevo: OncePerRequestFilter marca la
        // request ya filtrada y reusarla saltaria el filtro.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notes");
        request.setRemoteAddr(ip);
        return request;
    }

    private MockHttpServletResponse call(RateLimitFilter filter, String ip, boolean[] chainCalled) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(requestFrom(ip), response, chain);
        chainCalled[0] = chain.getRequest() != null;
        return response;
    }

    @Test
    void allowsRequestsUpToTheLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(3, 60, TestProblems.responder());
        boolean[] chainCalled = new boolean[1];

        for (int i = 1; i <= 3; i++) {
            MockHttpServletResponse response = call(filter, "10.0.0.1", chainCalled);
            assertThat(response.getStatus()).as("peticion %d", i).isEqualTo(200);
            assertThat(chainCalled[0]).isTrue();
        }
    }

    @Test
    void rejectsWithTooManyRequestsAndRetryAfterOnceExceeded() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, 60, TestProblems.responder());
        boolean[] chainCalled = new boolean[1];

        call(filter, "10.0.0.1", chainCalled);
        call(filter, "10.0.0.1", chainCalled);
        MockHttpServletResponse blocked = call(filter, "10.0.0.1", chainCalled);

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(chainCalled[0]).isFalse();
        assertThat(Integer.parseInt(blocked.getHeader(HttpHeaders.RETRY_AFTER)))
                .isBetween(1, 60);
    }

    @Test
    void countersAreIsolatedPerClientIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 60, TestProblems.responder());
        boolean[] chainCalled = new boolean[1];

        call(filter, "10.0.0.1", chainCalled);
        assertThat(call(filter, "10.0.0.1", chainCalled).getStatus()).isEqualTo(429);

        // Otra IP arranca con su propia ventana.
        assertThat(call(filter, "10.0.0.2", chainCalled).getStatus()).isEqualTo(200);
        assertThat(chainCalled[0]).isTrue();
    }

    @Test
    void windowResetsAfterItExpires() throws Exception {
        // Ventana de 1 segundo para poder observar el reinicio sin alargar la suite.
        RateLimitFilter filter = new RateLimitFilter(1, 1, TestProblems.responder());
        boolean[] chainCalled = new boolean[1];

        call(filter, "10.0.0.1", chainCalled);
        assertThat(call(filter, "10.0.0.1", chainCalled).getStatus()).isEqualTo(429);

        Thread.sleep(1100);

        assertThat(call(filter, "10.0.0.1", chainCalled).getStatus()).isEqualTo(200);
        assertThat(chainCalled[0]).isTrue();
    }
}
