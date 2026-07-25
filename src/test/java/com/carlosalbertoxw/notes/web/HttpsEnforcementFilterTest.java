package com.carlosalbertoxw.notes.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas unitarias del redirect HTTP -> HTTPS y de la cabecera HSTS. */
class HttpsEnforcementFilterTest {

    private final HttpsEnforcementFilter filter = new HttpsEnforcementFilter();

    private static MockHttpServletRequest request(boolean secure) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notes");
        request.setServerName("api.ejemplo.com");
        request.setSecure(secure);
        return request;
    }

    @Test
    void redirectsInsecureRequestsWithPermanentRedirect() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(false), response, chain);

        assertThat(response.getStatus()).isEqualTo(308);
        assertThat(response.getHeader("Location")).isEqualTo("https://api.ejemplo.com/api/notes");
        assertThat(chain.getRequest()).as("no debe seguir la cadena").isNull();
    }

    @Test
    void redirectPreservesQueryString() throws Exception {
        MockHttpServletRequest request = request(false);
        request.setQueryString("pageSize=5&search=zanahoria");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Location"))
                .isEqualTo("https://api.ejemplo.com/api/notes?pageSize=5&search=zanahoria");
    }

    @Test
    void addsHstsHeaderAndContinuesWhenAlreadySecure() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(true), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
    }
}
