package com.carlosalbertoxw.notes.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de las peticiones de creacion y actualizacion de notas.
 * La validacion se aplica antes de llegar al controlador; un cuerpo invalido
 * responde 400 con detalle (RFC 7807 / ProblemDetail).
 */
public record NoteRequest(
        @NotBlank(message = "El titulo es obligatorio.")
        @Size(max = 250, message = "El titulo no puede superar los 250 caracteres.")
        String title,

        @Size(max = 100_000, message = "El contenido no puede superar los 100.000 caracteres.")
        String text) {
}
