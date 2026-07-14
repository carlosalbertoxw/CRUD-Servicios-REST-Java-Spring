package com.carlosalbertoxw.notes.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del rate limiting por IP (ventana fija).
 *
 * @param permitLimit   peticiones permitidas por ventana e IP (default 100).
 * @param windowSeconds tamano de la ventana en segundos (default 60).
 */
@ConfigurationProperties(prefix = "rate-limiting")
public record RateLimitProperties(Integer permitLimit, Integer windowSeconds) {

    public RateLimitProperties {
        if (permitLimit == null) {
            permitLimit = 100;
        }
        if (windowSeconds == null) {
            windowSeconds = 60;
        }
    }
}
