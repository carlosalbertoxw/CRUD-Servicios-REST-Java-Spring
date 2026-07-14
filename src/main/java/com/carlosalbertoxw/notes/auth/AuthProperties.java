package com.carlosalbertoxw.notes.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de la autenticacion por API key.
 *
 * @param keyCacheSeconds TTL del cache en memoria de keys validadas (default 60).
 *                        Una revocacion tarda hasta este tiempo en surtir efecto.
 */
@ConfigurationProperties(prefix = "authentication")
public record AuthProperties(Integer keyCacheSeconds) {

    public AuthProperties {
        if (keyCacheSeconds == null) {
            keyCacheSeconds = 60;
        }
    }
}
