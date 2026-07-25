package com.carlosalbertoxw.notes.web;

import com.carlosalbertoxw.notes.support.TestProblems;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas unitarias del escritor de errores RFC 7807 usado por los filtros. */
class ProblemResponderTest {

    private final ProblemResponder problems = TestProblems.responder();

    @Test
    void writesProblemJsonWithStatusAndDetail() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        problems.write(response, HttpStatus.NOT_FOUND, "La nota no existe.");

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<?, ?> body = TestProblems.JSON.readValue(response.getContentAsString(), Map.class);
        assertThat(body.get("status")).isEqualTo(404);
        assertThat(body.get("detail")).isEqualTo("La nota no existe.");
    }

    @Test
    void encodesNonAsciiDetailAsUtf8() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        problems.write(response, HttpStatus.BAD_REQUEST, "Petición inválida: año");

        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        Map<?, ?> body = TestProblems.JSON.readValue(response.getContentAsString(), Map.class);
        assertThat(body.get("detail")).isEqualTo("Petición inválida: año");
    }
}
