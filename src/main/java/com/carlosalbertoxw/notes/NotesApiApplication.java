package com.carlosalbertoxw.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada de la API de notas.
 *
 * <p>Servicio REST con autenticacion por API key (hasheada, con revocacion y
 * expiracion), notas privadas por cliente, migraciones de esquema, paginacion
 * por keyset, busqueda de texto completo, rate limiting, health checks y
 * documentacion OpenAPI interactiva.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class NotesApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotesApiApplication.class, args);
    }
}
