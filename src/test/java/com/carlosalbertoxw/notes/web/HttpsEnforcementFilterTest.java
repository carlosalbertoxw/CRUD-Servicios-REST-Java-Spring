package com.carlosalbertoxw.notes.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.carlosalbertoxw.notes.support.TestProblems;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Pruebas unitarias del redirect HTTP -> HTTPS, de la lista de hosts y de HSTS. */
class HttpsEnforcementFilterTest {

    private final HttpsEnforcementFilter filter =
            new HttpsEnforcementFilter(List.of(" api.ejemplo.com ", ""), TestProblems.responder());

    private static MockHttpServletRequest request(boolean secure, String host) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notes");
        request.setServerName(host);
        request.setSecure(secure);
        return request;
    }

    @Test
    void redirectsInsecureRequestsWithPermanentRedirect() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(false, "api.ejemplo.com"), response, chain);

        assertThat(response.getStatus()).isEqualTo(308);
        assertThat(response.getHeader("Location")).isEqualTo("https://api.ejemplo.com/api/notes");
        assertThat(chain.getRequest()).as("no debe seguir la cadena").isNull();
    }

    @Test
    void redirectPreservesQueryString() throws Exception {
        MockHttpServletRequest request = request(false, "api.ejemplo.com");
        request.setQueryString("pageSize=5&search=zanahoria");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Location"))
                .isEqualTo("https://api.ejemplo.com/api/notes?pageSize=5&search=zanahoria");
    }

    @Test
    void redirectUsesTheConfiguredHostRegardlessOfCase() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(false, "API.Ejemplo.COM"), response, new MockFilterChain());

        assertThat(response.getHeader("Location")).isEqualTo("https://api.ejemplo.com/api/notes");
    }

    @Test
    void rejectsHostsOutsideTheAllowListInsteadOfRedirecting() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(false, "evil.example"), response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void withoutAllowedHostsEveryInsecureRequestIsRejected() throws Exception {
        HttpsEnforcementFilter noHosts = new HttpsEnforcementFilter(List.of(), TestProblems.responder());
        MockHttpServletResponse response = new MockHttpServletResponse();

        noHosts.doFilter(request(false, "api.ejemplo.com"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void addsHstsHeaderAndContinuesWhenAlreadySecure() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(true, "cualquier.host"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Strict-Transport-Security")).isEqualTo("max-age=31536000; includeSubDomains");
    }
}
