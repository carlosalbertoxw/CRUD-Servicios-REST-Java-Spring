package com.carlosalbertoxw.notes.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Valida al arranque que la cadena de conexion este presente: si falta, la
 * instancia no inicia (en vez de fallar mas tarde con trafico). Equivale a un
 * {@code ValidateOnStart} sobre la configuracion de base de datos.
 */
@ConfigurationProperties(prefix = "spring.datasource")
@Validated
public record DatasourceProperties(
        @NotBlank(message = "Falta la cadena de conexion 'spring.datasource.url' (variable DB_URL).")
        String url) {
}
