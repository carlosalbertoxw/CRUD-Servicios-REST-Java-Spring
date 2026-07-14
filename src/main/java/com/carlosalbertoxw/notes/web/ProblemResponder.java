package com.carlosalbertoxw.notes.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Escribe respuestas de error en formato RFC 7807 ({@link ProblemDetail}) desde
 * los filtros (auth y rate limiting), que estan fuera del pipeline de MVC y por
 * tanto no se benefician del manejo automatico de {@code spring.mvc.problemdetails}.
 */
@Component
public class ProblemResponder {

    private final ObjectMapper objectMapper;

    public ProblemResponder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
