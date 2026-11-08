package com.carlosalbertoxw.notes.model;

import java.time.Instant;

/**
 * Nota con su contenido completo. Las fechas son instantes UTC y se serializan
 * en ISO-8601 con sufijo {@code Z} (p. ej. {@code 2026-11-07T10:00:00Z}).
 */
public record Note(int id, String title, String text, Instant createdAt, Instant updatedAt) {}
