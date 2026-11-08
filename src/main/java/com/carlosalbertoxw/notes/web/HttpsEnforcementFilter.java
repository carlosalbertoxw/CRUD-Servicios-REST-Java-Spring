package com.carlosalbertoxw.notes.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Endurecimiento de transporte para produccion (se activa con
 * {@code app.security.https-enforced=true}, por defecto fuera del perfil dev):
 * redirige HTTP -> HTTPS (308) y emite HSTS. Asume TLS terminado en un reverse
 * proxy; el esquema real se toma de {@code X-Forwarded-Proto} via el RemoteIpValve
 * de Tomcat ({@code server.forward-headers-strategy=native}), que solo confia en
 * proxies declarados.
 *
 * <p>La redireccion solo se hace hacia un host de {@code app.security.allowed-hosts}:
 * el {@code Host} lo envia el cliente, y construir el {@code Location} con el sin
 * validarlo permitiria redirigir a un dominio ajeno (open redirect). Un host que no
 * esta en la lista recibe 400.
 */
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L; // 1 anio

    private final List<String> allowedHosts;
    private final ProblemResponder problems;

    public HttpsEnforcementFilter(List<String> allowedHosts, ProblemResponder problems) {
        this.allowedHosts = allowedHosts.stream()
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .toList();
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!request.isSecure()) {
            Optional<String> host = allowedHost(request.getServerName());
            if (host.isEmpty()) {
                problems.write(response, HttpStatus.BAD_REQUEST, "Host no permitido; usa HTTPS con un host valido.");
                return;
            }
            String query = request.getQueryString();
            // El host sale de la configuracion, no de la peticion.
            String target = "https://" + host.get() + request.getRequestURI() + (query != null ? "?" + query : "");
            response.setStatus(308); // Permanent Redirect
            response.setHeader("Location", target);
            return;
        }

        response.setHeader("Strict-Transport-Security", "max-age=" + HSTS_MAX_AGE_SECONDS + "; includeSubDomains");
        chain.doFilter(request, response);
    }

    /** Devuelve el host configurado que coincide (sin distinguir mayusculas), si lo hay. */
    private Optional<String> allowedHost(String requestedHost) {
        return allowedHosts.stream()
                .filter(allowed -> allowed.equalsIgnoreCase(requestedHost))
                .findFirst();
    }
}
