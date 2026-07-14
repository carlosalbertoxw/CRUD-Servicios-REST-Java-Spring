package com.carlosalbertoxw.notes.auth;

import com.carlosalbertoxw.notes.web.ProblemResponder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

/**
 * Autenticacion por API key: el cliente envia
 * {@code X-Api-Key: <key_id>.<secreto>}; se busca la key activa por key_id y se
 * compara el SHA-256 del secreto en tiempo constante.
 *
 * <p><b>Fail-closed:</b> todo endpoint exige key valida salvo la lista blanca
 * (documentacion, health checks y la raiz). Sin key valida (o con una revocada
 * o expirada) responde 401 con {@code WWW-Authenticate: ApiKey}.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Api-Key";
    public static final String SCHEME_NAME = "ApiKey";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    // Rutas exentas de autenticacion (documentacion, health checks y raiz).
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/actuator/health", "/v3/api-docs", "/swagger-ui");

    private final ApiKeyRepository apiKeys;
    private final ProblemResponder problems;

    public ApiKeyAuthFilter(ApiKeyRepository apiKeys, ProblemResponder problems) {
        this.apiKeys = apiKeys;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (isPublic(request)) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HEADER_NAME);
        if (header == null || header.isBlank()) {
            challenge(response, "Falta el encabezado '" + HEADER_NAME + ": <key_id>.<secreto>'.");
            return;
        }

        String[] parts = header.split("\\.", 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            challenge(response, "Formato invalido; se espera '" + HEADER_NAME + ": <key_id>.<secreto>'.");
            return;
        }

        Optional<ApiKey> apiKey = apiKeys.getActiveKey(parts[0]);
        if (apiKey.isEmpty() || !secretMatches(parts[1], apiKey.get().keyHash())) {
            log.info("Intento de autenticacion fallido para key_id {}", parts[0]);
            challenge(response, "API key invalida, revocada o expirada.");
            return;
        }

        ApiKey key = apiKey.get();
        try {
            apiKeys.registerUse(key.keyId());
        } catch (Exception ex) {
            // Telemetria de mejor esfuerzo: no debe tumbar una autenticacion valida.
            log.warn("No se pudo registrar el uso de la key {}", key.keyId(), ex);
        }

        request.setAttribute(
                AuthenticatedClient.REQUEST_ATTRIBUTE,
                new AuthenticatedClient(key.keyId(), key.clientName()));
        chain.doFilter(request, response);
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/") || path.equals("/favicon.ico")) {
            return true;
        }
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void challenge(HttpServletResponse response, String detail) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, SCHEME_NAME);
        problems.write(response, HttpStatus.UNAUTHORIZED, detail);
    }

    private static boolean secretMatches(String providedSecret, byte[] storedHash) {
        byte[] providedHash = sha256(providedSecret);
        // MessageDigest.isEqual es de tiempo constante en JDK modernos.
        return MessageDigest.isEqual(providedHash, storedHash);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
