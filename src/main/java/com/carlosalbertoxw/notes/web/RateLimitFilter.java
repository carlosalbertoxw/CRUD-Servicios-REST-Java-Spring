package com.carlosalbertoxw.notes.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting por IP con ventana fija: hasta {@code permitLimit} peticiones
 * por ventana de {@code windowSeconds}. Al excederlo responde 429 con
 * {@code Retry-After}. Cada IP es una particion independiente (equivalente al
 * {@code FixedWindowLimiter} por IP del proyecto de referencia).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final int permitLimit;
    private final long windowMillis;
    private final ProblemResponder problems;

    private final ConcurrentHashMap<String, Window> counters = new ConcurrentHashMap<>();
    private final AtomicLong lastSweep = new AtomicLong(System.currentTimeMillis());

    public RateLimitFilter(int permitLimit, int windowSeconds, ProblemResponder problems) {
        this.permitLimit = permitLimit;
        this.windowMillis = windowSeconds * 1000L;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long now = System.currentTimeMillis();
        sweepIfDue(now);

        String ip = clientIp(request);
        Window window = counters.compute(ip, (key, current) -> {
            if (current == null || now - current.start >= windowMillis) {
                return new Window(now, 1);
            }
            current.count++;
            return current;
        });

        if (window.count > permitLimit) {
            long retryAfter = Math.max(1, (windowMillis - (now - window.start) + 999) / 1000);
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
            problems.write(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Se supero el limite de peticiones. Reintenta en " + retryAfter + " segundos.");
            return;
        }

        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        // Detras de un reverse proxy confiable, server.forward-headers-strategy hace
        // que getRemoteAddr() ya refleje el X-Forwarded-For real.
        String ip = request.getRemoteAddr();
        return ip != null ? ip : "unknown";
    }

    /** Elimina de forma oportunista las particiones cuya ventana ya expiro. */
    private void sweepIfDue(long now) {
        long last = lastSweep.get();
        if (now - last >= windowMillis && lastSweep.compareAndSet(last, now)) {
            counters.values().removeIf(w -> now - w.start >= windowMillis);
        }
    }

    private static final class Window {
        final long start;
        int count;

        Window(long start, int count) {
            this.start = start;
            this.count = count;
        }
    }
}
