package com.carlosalbertoxw.notes.auth;

/**
 * Identidad del cliente autenticado, adjuntada a la peticion por
 * {@link ApiKeyAuthFilter} y consumida por los controladores para acotar las
 * operaciones a las notas de ese {@code keyId}.
 */
public record AuthenticatedClient(String keyId, String clientName) {

    /** Nombre del atributo de request donde el filtro deja la identidad. */
    public static final String REQUEST_ATTRIBUTE = "authenticatedClient";
}
