# CRUD-Servicios-REST-Java-Spring

Servicio REST con **Spring Boot 3** (Java 17): un CRUD de notas respaldado por
MySQL, con autenticación por API key (hasheada, con revocación y expiración),
notas privadas por cliente, migraciones de base de datos, paginación por keyset,
búsqueda de texto completo, rate limiting, health checks, pruebas de integración
y documentación OpenAPI interactiva.

## Requisitos

- [Docker](https://www.docker.com/) — suficiente para levantar todo el stack (Opción A) y para las pruebas de integración.
- [JDK 17+](https://adoptium.net/) y [Maven](https://maven.apache.org/) — solo para desarrollo local con el SDK (Opción B).

## Puesta en marcha

### Opción A — Todo en Docker

Levanta MySQL, aplica las migraciones (con datos de ejemplo) y arranca la API con un solo comando:

```bash
docker compose up --build
```

`docker compose` orquesta el orden: espera a que MySQL esté sano, corre el
servicio `migrations` (que aplica el esquema con el usuario de DDL y termina) y
solo entonces arranca `api` (que sirve con el usuario de mínimo privilegio). La
API queda en `http://localhost:8080` (ver [endpoints](#endpoints)).

### Opción B — Desarrollo local con el SDK

**1. Levantar la base de datos** (MySQL 8.4 en Docker):

```bash
docker compose up -d mysql
```

> Si ya tienes un contenedor MySQL con la base `notes` corriendo en `localhost:3306`,
> puedes saltarte este paso: la app se conecta a él por defecto.

**2. Aplicar las migraciones** (perfil `migrate`: corre Flyway con el usuario de DDL y termina):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=migrate
```

> Contra una base ya provisionada (p. ej. el contenedor existente) este paso es
> idempotente y no hace nada; en una base nueva crea el esquema.

**3. Ejecutar la API** con el perfil `dev` (crea la API key de desarrollo y notas de ejemplo, y sirve por HTTP plano):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

La API queda disponible en `http://localhost:8080`.

### Endpoints

- **Documentación interactiva (Swagger UI):** `/swagger-ui.html`
- **Especificación OpenAPI:** `/v3/api-docs`
- **Health checks:** `/actuator/health/liveness` (proceso vivo) y `/actuator/health/readiness` (BD alcanzable)

## Autenticación

Todos los endpoints exigen el encabezado `X-Api-Key` (*fail-closed*: solo la
documentación, los health checks y la raíz están exentos). El formato de la key
es `<key_id>.<secreto>`:

```
X-Api-Key: local-dev.dev-secret        <-- key de desarrollo creada por el perfil dev
```

Las keys viven en la tabla `api_keys` con el **secreto hasheado** (SHA-256,
`BINARY(32)`), lo que permite tener varias keys (una por cliente), revocarlas
individualmente (`revoked_at`), darles caducidad (`expires_at`) y detectar keys
en desuso (`last_used_at`, actualizado de forma laxa, máx. una escritura por
hora). **Cada cliente solo ve y modifica sus propias notas** (columna
`owner_key_id`). Para administrar keys:

```sql
-- Crear: el cliente usará "X-Api-Key: cliente-1.<secreto-aleatorio-largo>"
INSERT INTO api_keys (key_id, key_hash, client_name, expires_at)
VALUES ('cliente-1', UNHEX(SHA2('<secreto-aleatorio-largo>', 256)), 'Nombre del cliente',
        UTC_TIMESTAMP() + INTERVAL 1 YEAR);

-- Revocar
UPDATE api_keys SET revoked_at = UTC_TIMESTAMP() WHERE key_id = 'cliente-1';

-- Detectar keys sin uso en 90 días
SELECT key_id, client_name, last_used_at FROM api_keys
WHERE last_used_at IS NULL OR last_used_at < UTC_TIMESTAMP() - INTERVAL 90 DAY;
```

Notas operativas:

- **Las keys se revocan, no se eliminan:** la FK de `notes.owner_key_id` impide el `DELETE` físico de una key con notas (a propósito — la revocación con `revoked_at` conserva la trazabilidad).
- **Cache de keys:** las keys validadas se cachean en memoria (`authentication.key-cache-seconds`, 60 s por defecto), así el caso caliente autentica sin tocar la BD. El costo: una revocación tarda hasta ese TTL en surtir efecto.

Sin key válida (o con una revocada/expirada) la API responde `401`. Además hay
**rate limiting** por IP (100 peticiones/minuto por defecto, configurable): al
excederlo responde `429` con `Retry-After`.

## Endpoints

Todas las operaciones actúan **sobre las notas del cliente autenticado**; las
notas de otros clientes responden `404`.

| Método | Ruta | Descripción | Respuestas |
|--------|------|-------------|------------|
| GET | `/api/notes?pageSize=&afterId=&search=` | Lista notas (id, título y fechas) | 200, 400 |
| GET | `/api/notes/{id}` | Obtiene una nota con su contenido completo | 200, 404 |
| POST | `/api/notes` | Crea una nota | 201, 400 |
| PUT | `/api/notes/{id}` | Actualiza una nota | 204, 400, 404 |
| DELETE | `/api/notes/{id}` | Elimina una nota | 204, 404 |

El cuerpo de `POST` y `PUT` es JSON con `title` (obligatorio, máx. 250
caracteres) y `text` (opcional, máx. 100.000 caracteres). Las respuestas
incluyen `createdAt` y `updatedAt` (UTC). Los errores usan el formato RFC 7807
(`application/problem+json`). Los cuerpos mayores a ~1 MB
(`app.max-request-body-bytes`) se rechazan con `413`.

El listado usa **paginación por keyset** (costo constante en la BD sin importar
la profundidad): `pageSize` (1–100, por defecto 20) y `afterId` (el `nextAfterId`
que devolvió la página anterior; `nextAfterId: null` indica que no hay más). Con
`search=` se filtra por **texto completo** sobre título y contenido (índice
FULLTEXT; los términos de menos de 3 caracteres y las *stopwords* de MySQL no
coinciden, y los resultados se ordenan por `id`, no por relevancia):

```bash
# Listar notas (primera página)
curl -H "X-Api-Key: local-dev.dev-secret" "http://localhost:8080/api/notes?pageSize=20"

# Página siguiente (usando nextAfterId de la respuesta anterior)
curl -H "X-Api-Key: local-dev.dev-secret" "http://localhost:8080/api/notes?pageSize=20&afterId=20"

# Buscar notas
curl -H "X-Api-Key: local-dev.dev-secret" "http://localhost:8080/api/notes?search=zanahoria"

# Crear una nota
curl -X POST http://localhost:8080/api/notes \
  -H "X-Api-Key: local-dev.dev-secret" \
  -H "Content-Type: application/json" \
  -d '{"title":"Mi nota","text":"Contenido de la nota."}'

# Actualizar la nota 1
curl -X PUT http://localhost:8080/api/notes/1 \
  -H "X-Api-Key: local-dev.dev-secret" \
  -H "Content-Type: application/json" \
  -d '{"title":"Título nuevo","text":"Contenido nuevo."}'

# Eliminar la nota 1
curl -X DELETE http://localhost:8080/api/notes/1 -H "X-Api-Key: local-dev.dev-secret"
```

## Pruebas

Hay dos suites, separadas por convención de nombre y por plugin:

| Suite | Clases | Plugin / fase | Requiere Docker |
|-------|--------|---------------|-----------------|
| Unitarias | `*Test` | Surefire (`test`) | No |
| Integración | `*IT` | Failsafe (`verify`) | Sí |

```bash
# Solo unitarias (rápido, sin Docker)
mvn test
```

```bash
# Ambas suites
mvn verify
```

```bash
# Solo las de integración
mvn verify -DskipUnitTests=true
```

Las **pruebas unitarias** cubren la lógica que no necesita infraestructura:
autenticación por API key (formato del encabezado, rutas públicas, secreto
incorrecto, identidad publicada en la petición), cache de keys, rate limiting por
IP, límite de tamaño del cuerpo, forzado de HTTPS/HSTS, respuestas RFC 7807 y las
restricciones de validación del cuerpo de petición.

Las **pruebas de integración** levantan un MySQL efímero con
[Testcontainers](https://testcontainers.com/) (requiere Docker), le aplican las
migraciones reales (Flyway) y ejercitan la API completa: autenticación, ciclo
CRUD, validaciones, paginación, búsqueda, aislamiento por cliente y health
checks.

En cada push, GitHub Actions ([.github/workflows/ci.yml](.github/workflows/ci.yml))
corre cada suite en su propio job (`unit-tests` e `integration-tests`, en
paralelo) y audita dependencias vulnerables.

## Configuración

| Propiedad | Variable de entorno | Descripción |
|-----------|---------------------|-------------|
| `spring.datasource.url` / `username` / `password` | `DB_URL` / `DB_USER` / `DB_PASSWORD` | Conexión de la aplicación a MySQL (usuario de mínimo privilegio, solo DML). Por defecto apunta al MySQL local en `localhost:3306`. |
| `spring.flyway.url` / `user` / `password` | `FLYWAY_DB_URL` / `FLYWAY_DB_USER` / `FLYWAY_DB_PASSWORD` | Conexión de las migraciones (usuario con privilegios de DDL). |
| `rate-limiting.permit-limit` | `RATE_LIMITING_PERMIT_LIMIT` | Peticiones permitidas por ventana e IP (default 100). |
| `rate-limiting.window-seconds` | `RATE_LIMITING_WINDOW_SECONDS` | Tamaño de la ventana en segundos (default 60). |
| `authentication.key-cache-seconds` | `AUTHENTICATION_KEY_CACHE_SECONDS` | TTL del cache en memoria de API keys (default 60). |
| `app.security.https-enforced` | `APP_SECURITY_HTTPS_ENFORCED` | Redirige HTTP→HTTPS (308) y emite HSTS (default `true`; el perfil `dev` lo pone en `false`). |
| `app.max-request-body-bytes` | `APP_MAX_REQUEST_BODY_BYTES` | Tamaño máximo del cuerpo de la petición (default 1048576). |
| `server.tomcat.remoteip.internal-proxies` | `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` | Regex de IPs de reverse proxies confiables para leer `X-Forwarded-*` (default: rangos privados). |

## Base de datos

- **Mínimo privilegio:** la aplicación que sirve tráfico se conecta como `notes_app` (solo `SELECT/INSERT/UPDATE/DELETE`, sin DDL — lo aprovisiona [db/init-users.sql](db/init-users.sql)) y **no gestiona el esquema**; las migraciones usan `notes_user`, que sí tiene privilegios de DDL. Así el proceso de la API nunca dispone de credenciales capaces de alterar tablas: un bug o inyección hipotética no puede tocar el esquema.
- **Migraciones como paso aparte:** el esquema vive en [src/main/resources/db/migration](src/main/resources/db/migration) y Flyway lo aplica con el perfil `migrate` (`java -jar notes-api.jar --spring.profiles.active=migrate`), que corre las migraciones y termina — en Docker Compose es el servicio `migrations`. Es idempotente (`IF NOT EXISTS`) y usa *baseline-on-migrate*, de modo que convive con una base ya provisionada.
- **Fechas en UTC:** el servidor MySQL corre con `--default-time-zone=+00:00` y las columnas son `DATETIME` (sin el límite de 2038 de `TIMESTAMP`); la conversión a hora local es responsabilidad del consumidor.
- **Pool de conexiones:** HikariCP limitado a 20 conexiones; con varias réplicas de la API, cuida que `réplicas × pool` quede por debajo del `max_connections` de MySQL (151 por defecto).
- **Índices:** PK en `id`, índice por `owner_key_id` (implícito en la FK) para el listado por cliente, y FULLTEXT en `(title, text)` para la búsqueda.

**Producción:** inyecta las credenciales desde el gestor de secretos de la
plataforma (variables de entorno, Vault, secretos de Kubernetes) — nunca las
commitees. Ejecuta las migraciones como paso de despliegue
(`--spring.profiles.active=migrate`) antes de arrancar la API. La app asume TLS
terminado en un reverse proxy: por defecto (`app.security.https-enforced=true`)
redirige HTTP→HTTPS y emite HSTS, y con `server.forward-headers-strategy=native`
lee `X-Forwarded-*` **solo desde proxies confiables** — declara los tuyos en
`server.tomcat.remoteip.internal-proxies` para que la IP del cliente (rate
limiting) y el esquema sean los reales. El perfil `dev` (que siembra la key de
desarrollo y desactiva el forzado de HTTPS) **no debe activarse en producción**:
allí las keys se crean con SQL.

## Estructura del proyecto

```
├── docker-compose.yml               # Stack completo: MySQL 8.4 (UTC) + API
├── Dockerfile                       # Imagen de la API (build Maven -> runtime JRE)
├── db/init-users.sql                # Usuario de aplicación con mínimo privilegio
├── .github/workflows/ci.yml         # CI: unitarias + integración + auditoría de dependencias
├── src/main/java/com/carlosalbertoxw/notes/
│   ├── auth/                        # API key (hash SHA-256, revocación, cache, fail-closed)
│   ├── config/                      # Filtros, resolver, OpenAPI, seed de desarrollo
│   ├── data/                        # Repositorio de notas (Spring JDBC)
│   ├── model/                       # Note, NoteRequest, NoteSummary, PagedResult
│   └── web/                         # NotesController, rate limiting, ProblemDetail
├── src/main/resources/
│   ├── application.yml              # Configuración
│   └── db/migration/                # Migraciones Flyway (esquema)
└── src/test/                        # Pruebas: *Test (unitarias) y *IT (Testcontainers)
```

Tecnologías: Spring Boot 3 (Spring Web MVC, Spring JDBC, Actuator, Validation),
MySQL, [Flyway](https://flywaydb.org/) para migraciones,
[Testcontainers](https://testcontainers.com/) para pruebas y
[springdoc-openapi](https://springdoc.org/) para la documentación.
