package com.carlosalbertoxw.notes.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rechaza con 413 las peticiones cuyo cuerpo (por Content-Length) supera el
 * limite configurado. Las notas se limitan a 100.000 caracteres; no hay razon
 * para aceptar cuerpos mayores a ~1 MB.
 */
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;
    private final ProblemResponder problems;

    public RequestSizeLimitFilter(long maxBytes, ProblemResponder problems) {
        this.maxBytes = maxBytes;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long contentLength = request.getContentLengthLong();
        if (contentLength > maxBytes) {
            problems.write(response, HttpStatus.PAYLOAD_TOO_LARGE,
                    "El cuerpo de la peticion supera el maximo de " + maxBytes + " bytes.");
            return;
        }
        chain.doFilter(request, response);
    }
}
