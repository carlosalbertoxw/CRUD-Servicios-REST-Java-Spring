package com.carlosalbertoxw.notes.web;

import com.carlosalbertoxw.notes.support.TestProblems;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas unitarias del corte por tamano de cuerpo (Content-Length). */
class RequestSizeLimitFilterTest {

    private static final long MAX_BYTES = 1_000;

    private final RequestSizeLimitFilter filter =
            new RequestSizeLimitFilter(MAX_BYTES, TestProblems.responder());

    private static MockHttpServletRequest postOf(int bytes) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/notes");
        request.setContent(new byte[bytes]);
        return request;
    }

    @Test
    void letsThroughBodiesWithinTheLimit() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(postOf((int) MAX_BYTES), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsOversizedBodyWithContentTooLarge() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(postOf((int) MAX_BYTES + 1), response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void letsThroughRequestsWithoutContentLength() throws Exception {
        // getContentLengthLong() devuelve -1 cuando no hay cuerpo declarado.
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/notes"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}
