package com.carlosalbertoxw.notes;

import com.carlosalbertoxw.notes.model.Note;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integracion de extremo a extremo: levantan un MySQL efimero con
 * Testcontainers, le aplican las migraciones reales (Flyway) y ejercitan la API
 * completa (autenticacion, ciclo CRUD, validaciones, paginacion, busqueda,
 * aislamiento por cliente y health checks).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class NotesApiIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("notes")
            .withCommand("--default-time-zone=+00:00");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        // En tests, Flyway aplica el esquema con las credenciales del contenedor
        // (tienen DDL); en produccion esto es un paso de despliegue aparte.
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.url", MYSQL::getJdbcUrl);
        registry.add("spring.flyway.user", MYSQL::getUsername);
        registry.add("spring.flyway.password", MYSQL::getPassword);
        // No forzar HTTPS en pruebas (se sirve por HTTP plano).
        registry.add("app.security.https-enforced", () -> "false");
    }

    private static final String DEV_KEY = "local-dev.dev-secret";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private HttpEntity<String> authed(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", DEV_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", DEV_KEY);
        return headers;
    }

    @Test
    void rejectsRequestsWithoutApiKey() {
        ResponseEntity<String> response = rest.getForEntity("/api/notes", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("ApiKey");
    }

    @Test
    void rejectsInvalidSecret() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "local-dev.wrong-secret");
        ResponseEntity<String> response = rest.exchange(
                "/api/notes", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void fullCrudCycle() {
        // Crear
        ResponseEntity<Note> created = rest.exchange("/api/notes", HttpMethod.POST,
                authed("{\"title\":\"Mi nota\",\"text\":\"Contenido\"}"), Note.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        Note note = created.getBody();
        assertThat(note).isNotNull();
        assertThat(note.id()).isPositive();
        assertThat(note.title()).isEqualTo("Mi nota");
        assertThat(note.createdAt()).isNotNull();

        int id = note.id();

        // Obtener
        ResponseEntity<Note> fetched = rest.exchange("/api/notes/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Note.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().text()).isEqualTo("Contenido");

        // Actualizar
        ResponseEntity<Void> updated = rest.exchange("/api/notes/" + id, HttpMethod.PUT,
                authed("{\"title\":\"Nuevo titulo\",\"text\":\"Nuevo contenido\"}"), Void.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Note> afterUpdate = rest.exchange("/api/notes/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Note.class);
        assertThat(afterUpdate.getBody().title()).isEqualTo("Nuevo titulo");

        // Eliminar
        ResponseEntity<Void> deleted = rest.exchange("/api/notes/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> afterDelete = rest.exchange("/api/notes/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsInvalidBodyWith400() {
        ResponseEntity<String> response = rest.exchange("/api/notes", HttpMethod.POST,
                authed("{\"title\":\"\",\"text\":\"sin titulo\"}"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void notFoundForMissingNote() {
        ResponseEntity<String> response = rest.exchange("/api/notes/999999", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void keysetPaginationReturnsCursor() {
        // Crea 3 notas propias para asegurar mas de una pagina con pageSize=1.
        for (int i = 0; i < 3; i++) {
            rest.exchange("/api/notes", HttpMethod.POST,
                    authed("{\"title\":\"Paginada " + i + "\"}"), Note.class);
        }
        ResponseEntity<Map> page = rest.exchange("/api/notes?pageSize=1", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = page.getBody();
        assertThat((java.util.List<?>) body.get("items")).hasSize(1);
        assertThat(body.get("nextAfterId")).isNotNull();
        assertThat(((Number) body.get("totalCount")).longValue()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void rejectsPageSizeOutOfRangeWith400() {
        ResponseEntity<String> response = rest.exchange("/api/notes?pageSize=500", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void fullTextSearchFindsMatchingNote() {
        rest.exchange("/api/notes", HttpMethod.POST,
                authed("{\"title\":\"Receta\",\"text\":\"lleva zanahoria fresca\"}"), Note.class);
        ResponseEntity<Map> result = rest.exchange("/api/notes?search=zanahoria", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((java.util.List<?>) result.getBody().get("items")).isNotEmpty();
    }

    @Test
    void notesAreIsolatedPerClient() {
        // Otra key con una nota propia, insertada directamente en la BD.
        jdbc.update("INSERT IGNORE INTO api_keys (key_id, key_hash, client_name) "
                + "VALUES ('other-client', UNHEX(SHA2('other-secret', 256)), 'Otro cliente')");
        jdbc.update("INSERT INTO notes (owner_key_id, title, text) "
                + "VALUES ('other-client', 'Nota ajena', 'privada')");
        Integer foreignId = jdbc.queryForObject(
                "SELECT id FROM notes WHERE owner_key_id = 'other-client' ORDER BY id DESC LIMIT 1",
                Integer.class);

        // El cliente local-dev no puede verla.
        ResponseEntity<String> response = rest.exchange("/api/notes/" + foreignId, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsOversizedBodyWith413() {
        String hugeText = "a".repeat(1_100_000); // > 1 MB
        ResponseEntity<String> response = rest.exchange("/api/notes", HttpMethod.POST,
                authed("{\"title\":\"grande\",\"text\":\"" + hugeText + "\"}"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void readinessHealthCheckIsPublicAndUp() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/health/readiness", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
}
