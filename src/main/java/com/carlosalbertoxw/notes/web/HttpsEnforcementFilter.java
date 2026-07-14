package com.carlosalbertoxw.notes.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Endurecimiento de transporte para produccion (se activa con
 * {@code app.security.https-enforced=true}, por defecto fuera del perfil dev):
 * redirige HTTP -> HTTPS (308) y emite HSTS. Asume TLS terminado en un reverse
 * proxy; el esquema real se toma de {@code X-Forwarded-Proto} via el RemoteIpValve
 * de Tomcat ({@code server.forward-headers-strategy=native}), que solo confia en
 * proxies declarados.
 */
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L; // 1 anio

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!request.isSecure()) {
            String query = request.getQueryString();
            String target = "https://" + request.getServerName() + request.getRequestURI()
                    + (query != null ? "?" + query : "");
            response.setStatus(308); // Permanent Redirect
            response.setHeader("Location", target);
            return;
        }

        response.setHeader("Strict-Transport-Security",
                "max-age=" + HSTS_MAX_AGE_SECONDS + "; includeSubDomains");
        chain.doFilter(request, response);
    }
}
