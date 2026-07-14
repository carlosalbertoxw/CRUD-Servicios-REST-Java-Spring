package com.carlosalbertoxw.notes.auth;

/**
 * Registro de una API key en la tabla {@code api_keys}. El secreto nunca se
 * almacena en claro: solo su hash SHA-256 ({@code BINARY(32)}).
 */
public record ApiKey(String keyId, byte[] keyHash, String clientName) {
}
