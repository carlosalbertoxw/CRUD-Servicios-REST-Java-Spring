package com.carlosalbertoxw.notes.model;

import java.time.LocalDateTime;

/**
 * Vista resumida para listados: no arrastra el contenido completo de la nota
 * (el texto se obtiene consultando la nota por id).
 */
public record NoteSummary(
        int id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
