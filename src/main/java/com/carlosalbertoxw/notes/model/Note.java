package com.carlosalbertoxw.notes.model;

import java.time.LocalDateTime;

/**
 * Nota con su contenido completo. Las fechas se guardan y devuelven en UTC
 * (la conversion a hora local es responsabilidad del consumidor).
 */
public record Note(
        int id,
        String title,
        String text,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
