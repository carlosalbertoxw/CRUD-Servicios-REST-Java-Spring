package com.carlosalbertoxw.notes.model;

import java.time.Instant;

/**
 * Vista resumida para listados: no arrastra el contenido completo de la nota
 * (el texto se obtiene consultando la nota por id).
 */
public record NoteSummary(int id, String title, Instant createdAt, Instant updatedAt) {}
