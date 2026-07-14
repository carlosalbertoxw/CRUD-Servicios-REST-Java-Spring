package com.carlosalbertoxw.notes.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Raiz de la API: punto de aterrizaje publico que apunta a la documentacion.
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "service", "notes-api",
                "status", "ok",
                "docs", "/swagger-ui.html",
                "openapi", "/v3/api-docs",
                "health", "/actuator/health");
    }
}
