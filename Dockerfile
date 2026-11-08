# Etapa de build: compila y empaqueta el jar (sin tests: las pruebas de
# integracion requieren Docker/Testcontainers, no disponible dentro del build).
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package \
    # Extrae el jar en capas (dependencias / aplicacion): un cambio de codigo
    # solo invalida la capa pequena de la aplicacion.
    && cp target/notes-api-*.jar app.jar     && java -Djarmode=tools -jar app.jar extract --layers --destination /extracted

# Etapa de runtime: imagen JRE minima con las capas extraidas.
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
