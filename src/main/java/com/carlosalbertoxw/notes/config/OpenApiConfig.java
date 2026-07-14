package com.carlosalbertoxw.notes.config;

import com.carlosalbertoxw.notes.auth.ApiKeyAuthFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacion OpenAPI: declara el esquema de API key para que la UI (Swagger)
 * permita configurar el encabezado {@code X-Api-Key} en las pruebas.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI notesOpenApi() {
        final String scheme = ApiKeyAuthFilter.SCHEME_NAME;
        return new OpenAPI()
                .info(new Info()
                        .title("Notes API")
                        .version("2.0.0")
                        .description("CRUD de notas con API key, paginacion, busqueda y observabilidad."))
                .components(new Components().addSecuritySchemes(scheme, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(ApiKeyAuthFilter.HEADER_NAME)
                        .description("API key con formato <key_id>.<secreto>")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
