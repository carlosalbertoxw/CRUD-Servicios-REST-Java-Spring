# CRUD-Servicios-REST-Java-Spring

Servicio REST con **Spring Boot 4** (Java 25): un CRUD de notas respaldado por
MySQL, con autenticación por API key (hasheada, con revocación y expiración),
notas privadas por cliente, migraciones de base de datos, paginación por keyset,
búsqueda de texto completo, rate limiting, health checks, pruebas de integración
y documentación OpenAPI interactiva.

## Requisitos

- [Docker](https://www.docker.com/) — suficiente para levantar todo el stack (Opción A) y para las pruebas de integración.
- [JDK 25](https://adoptium.net/) y [Maven](https://maven.apache.org/) — solo para desarrollo local con el SDK (Opción B).

## Puesta en marcha

### Opción A — Todo en Docker

Levanta MySQL, aplica las migraciones (con datos de ejemplo) y arranca la API con un solo comando:

```bash
docker compose up --build
```

`docker compose` orquesta el orden: espera a que MySQL esté sano, corre el
servicio `migrations` (la imagen oficial de Flyway, que aplica el esquema con el usuario de DDL y termina) y
solo entonces arranca `api` (que sirve con el usuario de mínimo privilegio). La
API queda en `http://localhost:8082` (ver [URLs útiles](#urls-útiles)).

> Los proyectos hermanos en C# y PHP publican MySQL y la API en otros puertos
> (3306/8080 y 3308/8081), así que los tres stacks pueden correr a la vez.

### Opción B — Desarrollo local con el SDK

**1. Levantar la base de datos** (MySQL 8.4 en Docker, publicado en `localhost:3307`
para no chocar con los proyectos C# y PHP):

```bash
docker compose up -d mysql
```

**2. Aplicar las migraciones** (imagen oficial de Flyway con el usuario de DDL; termina al acabar):

```bash
docker compose run --rm migrations
```

**3. Ejecutar la API** con el perfil `dev` (crea la API key de desarrollo y notas de ejemplo, y sirve por HTTP plano):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

La API queda disponible en `http://localhost:8080` (los ejemplos de abajo usan
este puerto; con la Opción A cámbialo por `8082`).

### URLs útiles

- **Raíz:** `/` (nombre del servicio y enlaces a la documentación y los health checks)
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

El secreto debe ser aleatorio y largo; por ejemplo, `openssl rand -hex 24`
genera uno de 48 caracteres.

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

Sin key válida (o con una revocada/expirada) la API responde `401` con
`WWW-Authenticate: ApiKey`. Además hay **rate limiting** por IP (100
peticiones/minuto por defecto, configurable): al excederlo responde `429` con
`Retry-After` (los segundos que faltan para que se renueve la ventana). Los
health checks no consumen cuota.

## Endpoints

Todas las operaciones actúan **sobre las notas del cliente autenticado**; las
notas de otros clientes responden `404`.

| Método | Ruta | Descripción | Respuestas |
|--------|------|-------------|------------|
| GET | `/api/notes?pageSize=&afterId=&search=` | Lista notas (id, título y fechas) | 200, 400 |
| GET | `/api/notes/{id}` | Obtiene una nota con su contenido completo | 200, 400, 404 |
| POST | `/api/notes` | Crea una nota (cabecera `Location` con su URL) | 201, 400, 413 |
| PUT | `/api/notes/{id}` | Actualiza una nota | 204, 400, 404, 413 |
| DELETE | `/api/notes/{id}` | Elimina una nota | 204, 400, 404 |

`{id}` es un entero: cualquier otro valor responde `400`. El cuerpo de `POST` y
`PUT` es JSON con `title` (obligatorio, no vacío ni solo espacios, máx. 250
caracteres) y `text` (opcional, máx. 100.000 caracteres). Las respuestas
incluyen `createdAt` y `updatedAt` en UTC con formato ISO-8601 y sufijo `Z`
(p. ej. `2026-11-07T10:00:00Z`); los campos sin valor se devuelven como `null`.
Los errores usan el formato **Problem Details** (RFC 9457,
`application/problem+json`). Los cuerpos mayores a ~1 MB
(`app.max-request-body-bytes`) se rechazan con `413`, también cuando llegan sin
`Content-Length` (`Transfer-Encoding: chunked`). Todas las respuestas llevan
`X-Content-Type-Options: nosniff`.

El listado usa **paginación por keyset** (costo constante en la BD sin importar
la profundidad): `pageSize` (1–100, por defecto 20) y `afterId` (el `nextAfterId`
que devolvió la página anterior; `nextAfterId: null` indica que no hay más). La
respuesta no incluye un total de registros: calcularlo exigiría un `COUNT(*)`
sobre todas las notas del cliente en cada página. Con
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
incorrecto, key_id publicado en la petición), cache de keys, rate limiting por
IP (incluida la exención de los health checks), límite de tamaño del cuerpo
(con y sin `Content-Length`), forzado de HTTPS/HSTS, respuestas Problem Details
y las restricciones de validación del cuerpo de petición.

Las **pruebas de integración** levantan un MySQL efímero con
[Testcontainers](https://testcontainers.com/) (requiere Docker), le aplican las
migraciones reales (Flyway, que en el proyecto solo es dependencia de pruebas)
y ejercitan la API completa: autenticación, ciclo CRUD, validaciones,
paginación, búsqueda, formato de fechas, cabeceras de seguridad, aislamiento
por cliente y health checks.

### Formato del código

El estilo lo aplica [Spotless](https://github.com/diffplug/spotless) con
palantir-java-format (4 espacios):

```bash
mvn spotless:check   # verifica (lo corre el CI)
```

```bash
mvn spotless:apply   # corrige
```

En cada push, GitHub Actions ([.github/workflows/ci.yml](.github/workflows/ci.yml))
corre la auditoría de dependencias, el formato + las unitarias y las de
integración. `integration-tests` depende de `unit-tests`: si la lógica básica
está rota, no se gasta un contenedor MySQL en confirmarlo. Además,
[CodeQL](.github/workflows/codeql.yml) analiza el código en cada push y una vez
por semana.

## Auditoría de dependencias

El job `dependency-audit` usa [OSV-Scanner](https://google.github.io/osv-scanner/):
resuelve el árbol de dependencias del [pom.xml](pom.xml) (directas y
transitivas), lo cruza con la base [OSV](https://osv.dev/) y **falla si
encuentra una vulnerabilidad conocida**. Tarda segundos, no necesita API key ni
caché.

En local (requiere el binario de [osv-scanner](https://google.github.io/osv-scanner/installation/)):

```bash
osv-scanner scan source --recursive .
```

Cuando la auditoría falla, de preferible a último recurso:

1. **Subir la dependencia directa** (o el parent de Spring Boot) a una versión ya parcheada.
2. **Forzar la versión de una transitiva** que aún no llega por el BOM de Spring
   Boot, con su propiedad de versión (p. ej. `<tomcat.version>`), y quitarla en
   cuanto Boot la incluya.
3. **Ignorar un falso positivo** con un `osv-scanner.toml` que documente por qué
   esa alerta no aplica.

| | Qué hace | Cadencia |
|---|---|---|
| [Dependabot](.github/dependabot.yml) | Abre PRs de actualización de versiones | Semanal |
| Dependabot (alertas) | Abre PRs ante un advisory de seguridad | Inmediato, sin depender del intervalo |
| OSV-Scanner | Bloquea el merge si una dependencia vulnerable llega al build | En cada run de CI |

## Configuración

| Propiedad | Variable de entorno | Descripción |
|-----------|---------------------|-------------|
| `spring.datasource.url` / `username` / `password` | `DB_URL` / `DB_USER` / `DB_PASSWORD` | Conexión de la aplicación a MySQL (usuario de mínimo privilegio, solo DML). Obligatoria: sin `DB_URL` la app no arranca. Solo el perfil `dev` trae valores por defecto (el MySQL de `docker-compose.yml` en `localhost:3307`). |
| `rate-limiting.permit-limit` | `RATE_LIMITING_PERMIT_LIMIT` | Peticiones permitidas por ventana e IP (default 100). |
| `rate-limiting.window-seconds` | `RATE_LIMITING_WINDOW_SECONDS` | Tamaño de la ventana en segundos (default 60). |
| `authentication.key-cache-seconds` | `AUTHENTICATION_KEY_CACHE_SECONDS` | TTL del cache en memoria de API keys (default 60). |
| `app.security.https-enforced` | `APP_SECURITY_HTTPS_ENFORCED` | Redirige HTTP→HTTPS (308) y emite HSTS (default `true`; el perfil `dev` lo pone en `false`). |
| `app.max-request-body-bytes` | `APP_MAX_REQUEST_BODY_BYTES` | Tamaño máximo del cuerpo de la petición (default 1048576). |
| `spring.threads.virtual.enabled` | `SPRING_THREADS_VIRTUAL_ENABLED` | Atiende cada petición en un hilo virtual (default `true`). |
| `server.tomcat.remoteip.internal-proxies` | `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` | Regex de IPs de reverse proxies confiables para leer `X-Forwarded-*` (default: rangos privados). |

Fuera de los perfiles `dev` y `test`, los logs salen en **JSON** (formato
[ECS](https://www.elastic.co/guide/en/ecs/current/index.html)) para que una
plataforma de logs los indexe sin parseo.

## Base de datos

- **Mínimo privilegio:** la aplicación que sirve tráfico se conecta como `notes_app` (solo `SELECT/INSERT/UPDATE/DELETE`, sin DDL — lo aprovisiona [db/init-users.sql](db/init-users.sql)) y **no gestiona el esquema**; las migraciones usan `notes_user`, que sí tiene privilegios de DDL. Así el proceso de la API nunca dispone de credenciales capaces de alterar tablas: un bug o inyección hipotética no puede tocar el esquema.
- **Migraciones como paso aparte:** el esquema vive en [src/main/resources/db/migration](src/main/resources/db/migration) y lo aplica la imagen oficial `flyway/flyway` (servicio `migrations` de Docker Compose), que corre las migraciones y termina. La aplicación no incluye Flyway en su classpath.
- **Fechas en UTC:** el servidor MySQL corre con `--default-time-zone=+00:00` y las columnas son `DATETIME` (sin el límite de 2038 de `TIMESTAMP`); la conversión a hora local es responsabilidad del consumidor.
- **Pool de conexiones:** HikariCP limitado a 20 conexiones; con varias réplicas de la API, cuida que `réplicas × pool` quede por debajo del `max_connections` de MySQL (151 por defecto).
- **Índices:** PK en `id`, índice por `owner_key_id` (implícito en la FK) para el listado por cliente, y FULLTEXT en `(title, text)` para la búsqueda.

**Producción:** inyecta las credenciales desde el gestor de secretos de la
plataforma (variables de entorno, Vault, secretos de Kubernetes) — nunca las
commitees. Ejecuta las migraciones como paso de despliegue (la imagen
`flyway/flyway` apuntando a `src/main/resources/db/migration`) antes de arrancar
la API. La app asume TLS
terminado en un reverse proxy: por defecto (`app.security.https-enforced=true`)
redirige HTTP→HTTPS y emite HSTS, y con `server.forward-headers-strategy=native`
lee `X-Forwarded-*` **solo desde proxies confiables** — declara los tuyos en
`server.tomcat.remoteip.internal-proxies` para que la IP del cliente (rate
limiting) y el esquema sean los reales. El perfil `dev` (que siembra la key de
desarrollo y desactiva el forzado de HTTPS) **no debe activarse en producción**:
allí las keys se crean con SQL.

## Estructura del proyecto

```
├── docker-compose.yml               # Stack completo: MySQL 8.4 (UTC) + Flyway + API
├── Dockerfile                       # Imagen de la API (build Maven -> runtime JRE 25, jar en capas)
├── db/init-users.sql                # Usuario de aplicación con mínimo privilegio
├── .github/workflows/               # CI (auditoría OSV, formato, unitarias, integración) y CodeQL
├── src/main/java/com/carlosalbertoxw/notes/
│   ├── auth/                        # API key (hash SHA-256, revocación, cache, fail-closed) y @CurrentKeyId
│   ├── config/                      # Registro de filtros y del resolver, OpenAPI, seed de desarrollo
│   ├── data/                        # Repositorio de notas (Spring JDBC)
│   ├── model/                       # Note, NoteRequest, NoteSummary, PagedResult
│   └── web/                         # Controladores, filtros (HTTPS, cabeceras, tamaño, rate limiting), ProblemDetail
├── src/main/resources/
│   ├── application.yml              # Configuración (application-dev.yml: valores de desarrollo)
│   └── db/migration/                # Migraciones Flyway (esquema)
└── src/test/                        # Pruebas: *Test (unitarias) y *IT (Testcontainers)
```

Tecnologías: Spring Boot 4 sobre Java 25 (Spring Web MVC, Spring JDBC, Actuator, Validation),
MySQL, [Flyway](https://flywaydb.org/) para migraciones,
[Testcontainers](https://testcontainers.com/) para pruebas y
[springdoc-openapi](https://springdoc.org/) para la documentación.

## Licencia

[MIT](LICENSE)
