package com.carlosalbertoxw.notes.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de las restricciones de validacion del cuerpo de peticion,
 * que son las que producen el 400 con ProblemDetail antes de llegar al
 * controlador.
 */
class NoteRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> violatedFields(NoteRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void acceptsATitleWithoutText() {
        assertThat(violatedFields(new NoteRequest("Mi nota", null))).isEmpty();
    }

    @Test
    void rejectsMissingTitle() {
        assertThat(violatedFields(new NoteRequest(null, "contenido"))).containsExactly("title");
    }

    @Test
    void rejectsBlankTitle() {
        assertThat(violatedFields(new NoteRequest("   ", "contenido"))).containsExactly("title");
    }

    @Test
    void rejectsTitleLongerThan250Characters() {
        assertThat(violatedFields(new NoteRequest("a".repeat(251), null))).containsExactly("title");
        assertThat(violatedFields(new NoteRequest("a".repeat(250), null))).isEmpty();
    }

    @Test
    void rejectsTextLongerThan100000Characters() {
        assertThat(violatedFields(new NoteRequest("Titulo", "a".repeat(100_001)))).containsExactly("text");
        assertThat(violatedFields(new NoteRequest("Titulo", "a".repeat(100_000)))).isEmpty();
    }
}
