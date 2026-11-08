package com.carlosalbertoxw.notes.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rechaza con 413 las peticiones cuyo cuerpo supera el limite configurado. Las
 * notas se limitan a 100.000 caracteres; no hay razon para aceptar cuerpos
 * mayores a ~1 MB.
 *
 * <ul>
 *   <li>Con {@code Content-Length}: se decide sin leer el cuerpo (Tomcat no
 *       entrega mas bytes de los declarados).</li>
 *   <li>Sin {@code Content-Length} ({@code Transfer-Encoding: chunked}): se lee
 *       como maximo {@code maxBytes + 1} bytes; si caben, la peticion sigue con
 *       el cuerpo ya leido.</li>
 * </ul>
 */
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final int maxBytes;
    private final ProblemResponder problems;

    public RequestSizeLimitFilter(long maxBytes, ProblemResponder problems) {
        this.maxBytes = Math.toIntExact(maxBytes);
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long contentLength = request.getContentLengthLong();
        if (contentLength > maxBytes) {
            reject(response);
            return;
        }
        if (contentLength >= 0) {
            chain.doFilter(request, response);
            return;
        }

        byte[] body = request.getInputStream().readNBytes(maxBytes + 1);
        if (body.length > maxBytes) {
            reject(response);
            return;
        }
        chain.doFilter(new BufferedBodyRequest(request, body), response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        problems.write(
                response,
                HttpStatus.CONTENT_TOO_LARGE,
                "El cuerpo de la peticion supera el maximo de " + maxBytes + " bytes.");
    }

    /** Peticion cuyo cuerpo ya se leyo (y valido) en memoria. */
    private static final class BufferedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        BufferedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public int read(byte[] buffer, int offset, int length) {
                    return in.read(buffer, offset, length);
                }

                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Lectura asincrona no soportada");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
