package com.carlosalbertoxw.notes.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Datos SOLO para desarrollo/pruebas (perfiles {@code dev} y {@code test}).
 * Crea la API key de desarrollo y notas de ejemplo de forma idempotente.
 *
 * <p>API key de desarrollo:  {@code X-Api-Key: local-dev.dev-secret}
 * (el hash se calcula en la BD con SHA2, no se guarda el secreto en claro).
 */
@Component
@Profile({"dev", "test"})
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final String DEV_KEY_ID = "local-dev";

    private final JdbcTemplate jdbc;

    public DevDataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.update(
                "INSERT IGNORE INTO api_keys (key_id, key_hash, client_name) "
                        + "VALUES (?, UNHEX(SHA2('dev-secret', 256)), ?)",
                DEV_KEY_ID, "Desarrollo local");

        Integer notesForDevKey = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notes WHERE owner_key_id = ?", Integer.class, DEV_KEY_ID);
        if (notesForDevKey != null && notesForDevKey == 0) {
            jdbc.update("INSERT INTO notes (owner_key_id, title, text) VALUES (?, ?, ?)",
                    DEV_KEY_ID, "Nota de ejemplo 1", "Contenido de la primera nota de ejemplo.");
            jdbc.update("INSERT INTO notes (owner_key_id, title, text) VALUES (?, ?, ?)",
                    DEV_KEY_ID, "Nota de ejemplo 2", "Contenido de la segunda nota de ejemplo.");
        }
        log.info("Datos de desarrollo listos (API key de dev: {}.dev-secret)", DEV_KEY_ID);
    }
}
