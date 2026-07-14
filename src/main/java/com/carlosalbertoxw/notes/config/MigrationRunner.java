package com.carlosalbertoxw.notes.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * En el perfil {@code migrate}, Flyway aplica el esquema durante el arranque del
 * contexto (antes de este runner) usando el usuario con privilegios de DDL. Este
 * runner solo cierra el proceso con codigo 0 para que sirva como paso de
 * despliegue discreto (en Docker Compose: {@code service_completed_successfully}).
 */
@Component
@Profile("migrate")
public class MigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

    private final ConfigurableApplicationContext context;

    public MigrationRunner(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Migraciones aplicadas. Finalizando el paso de migracion.");
        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
